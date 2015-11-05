package corpus.parser.bionlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import corpus.AnnotationConfig;
import corpus.BioNLPCorpus;
import corpus.SubDocument;
import corpus.parser.ParsingUtils;
import corpus.parser.bionlp.annotations.BratAnnotation;
import corpus.parser.bionlp.annotations.BratEventAnnotation;
import corpus.parser.bionlp.annotations.BratTextBoundAnnotation;
import corpus.parser.bionlp.exceptions.AnnotationFileException;
import corpus.parser.bionlp.exceptions.AnnotationReferenceMissingException;
import corpus.parser.bionlp.exceptions.AnnotationTextMismatchException;
import corpus.parser.bionlp.exceptions.AnnotationTypeMissingException;
import corpus.parser.bionlp.julie.Tokenization;
import utility.ID;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.EntityType;
import variables.State;

public class Brat2BIREConverter {

	private static Logger log = LogManager.getFormatterLogger(Brat2BIREConverter.class.getName());

	private static final Comparator<BratTextBoundAnnotation> textBoundAnnotationComparator = new Comparator<BratTextBoundAnnotation>() {

		@Override
		public int compare(BratTextBoundAnnotation a1, BratTextBoundAnnotation a2) {
			return a1.getStart() - a2.getStart();
		}

	};

	/**
	 * Converts the Brat Annotations, which are stored in the
	 * {@link BratAnnotationManager}, to the BIRE format. For each sentence in
	 * the given BratDocument/sentence file, an AnnotatedDocument is returned.
	 * 
	 * @param bratDoc
	 * @param corpus
	 * @param sentFile
	 * @param tokenizedTextFilepath
	 * @throws AnnotationFileException
	 * @throws Exception
	 */
	public static List<SubDocument> convert(BratAnnotatedDocument bratDoc, BioNLPCorpus corpus,
			List<Tokenization> tokenizations) throws AnnotationFileException {
		List<SubDocument> documents = new ArrayList<SubDocument>();
		log.debug("Split BratDocument %s in %s documents", bratDoc.getDocumentName(), tokenizations.size());
		Multimap<String, BratAnnotation> annotationsByFilename = bratDoc.getManager().getAnnotationsByFilename();

		List<BratTextBoundAnnotation> textAnnotationsA1 = new ArrayList<BratTextBoundAnnotation>();
		List<BratTextBoundAnnotation> textAnnotationsA2 = new ArrayList<BratTextBoundAnnotation>();
		List<BratEventAnnotation> eventAnnotationsA1 = new ArrayList<BratEventAnnotation>();
		List<BratEventAnnotation> eventAnnotationsA2 = new ArrayList<BratEventAnnotation>();

		for (String filename : annotationsByFilename.keySet()) {
			if (filename.endsWith(".a1")) {
				sortAnnotations(annotationsByFilename.get(filename), textAnnotationsA1, eventAnnotationsA1);
			} else if (filename.endsWith(".a2")) {
				sortAnnotations(annotationsByFilename.get(filename), textAnnotationsA2, eventAnnotationsA2);
			}
		}
		if (!eventAnnotationsA1.isEmpty()) {
			throw new AnnotationFileException(String.format("The a1 file of document %s contains event annotations: %s",
					bratDoc.getDocumentName(), eventAnnotationsA1));
		}

		int sentenceNumber = 0;
		for (Tokenization tokenization : tokenizations) {
			log.debug("############");
			log.debug("Tokens: %s", tokenization.tokens);
			SubDocument doc = new SubDocument(bratDoc.getDocumentName(),
					bratDoc.getDocumentName() + "-" + sentenceNumber, tokenization.originalSentence,
					tokenization.tokens, tokenization.absoluteStartOffset);

			State priorKnowledge = new State(doc);
			/*
			 * These annotations are provided for the event extraction task as
			 * initial knowledge. The state sees them as fixed.
			 */
			log.debug("Parse A1 annotations...");
			if (textAnnotationsA1.isEmpty()) {
				log.warn("The SubDocument %s has no A1 annotations", doc.getName());
			}
			for (BratTextBoundAnnotation tann : textAnnotationsA1) {
				if (isInSentence(tann, tokenization)) {
					try {
						EntityAnnotation entity = convertTextBoundAnnotation(priorKnowledge, corpus.getCorpusConfig(),
								tokenization, tann);
						entity.setFixed(true);
						priorKnowledge.addEntity(entity);
					} catch (Exception e) {
						log.warn(e);
						// e.printStackTrace();
					}
				}
			}

			State goldState = new State(priorKnowledge);
			/*
			 * The following annotations resemble the gold standard for this
			 * document which are the target of the event extraction task.
			 */
			log.debug("Parse A2 annotations...");
			for (BratTextBoundAnnotation tann : textAnnotationsA2) {
				if (isInSentence(tann, tokenization)) {
					try {
						EntityAnnotation entity = convertTextBoundAnnotation(goldState, corpus.getCorpusConfig(),
								tokenization, tann);
						goldState.addEntity(entity);
					} catch (Exception e) {
						log.warn(e);
					}
				}
			}

			for (BratEventAnnotation eann : eventAnnotationsA2) {
				if (isInSentence(eann.getTrigger(), tokenization)) {
					try {
						EntityAnnotation entity = convertEventAnnotation(goldState, corpus.getCorpusConfig(),
								tokenization, eann);
						goldState.addEntity(entity);
					} catch (Exception e) {
						log.warn(e);
					}
				}
			}
			try {
				checkConsistency(goldState);
				doc.setPriorKnowledge(priorKnowledge);
				doc.setGoldResult(goldState);
				documents.add(doc);
			} catch (Exception e) {
				log.warn(e);
				log.warn("--> Skip inconsistent document \"%s\"", doc.getName());
			}
			sentenceNumber++;
		}
		return documents;

	}

	/**
	 * Adds the annotations in allAnnotations to the given list with respect to
	 * their type.
	 * 
	 * @param allAnnotations
	 * @param textAnnotations
	 * @param eventAnnotations
	 */
	private static void sortAnnotations(Collection<BratAnnotation> allAnnotations,
			List<BratTextBoundAnnotation> textAnnotations, List<BratEventAnnotation> eventAnnotations) {
		for (BratAnnotation ann : allAnnotations) {
			if (ann instanceof BratTextBoundAnnotation) {
				textAnnotations.add((BratTextBoundAnnotation) ann);
			} else if (ann instanceof BratEventAnnotation) {
				eventAnnotations.add((BratEventAnnotation) ann);
			}
		}
		Collections.sort(textAnnotations, textBoundAnnotationComparator);
	}

	private static void checkConsistency(State state)
			throws AnnotationReferenceMissingException, AnnotationTextMismatchException {
		Set<VariableID> existingEntities = state.getEntityIDs();
		for (EntityAnnotation e : state.getEntities()) {
			// check if all arguments are present
			for (VariableID id : e.getReadOnlyArguments().values()) {
				if (!existingEntities.contains(id)) {
					log.warn("Entity %s references missing entity %s", e.getID(), id);
					throw new AnnotationReferenceMissingException(
							String.format("Entity %s references missing entity %s", e.getID(), id));
				}
			}

			if (!Objects.equals(e.getText(), e.getOriginalText())) {
				throw new AnnotationTextMismatchException(
						String.format("Token-level text and Character-level text for entity %s are not equal: %s != %s",
								e.getID(), e.getText(), e.getOriginalText()));
			}
			/*
			 * FIXME apparently some annotations span across multiple sentences
			 * (PMID-8051172: E4 Trigger:T10 Theme:T5, where T5 and T10 are not
			 * part of the same sentence)
			 */
			// for (int index = e.getBeginTokenIndex(); index <
			// e.getEndTokenIndex(); index++) {
			// if (!state.getAnnotationsForToken(index).contains(e.getID())) {
			// throw new AnnotationTextMismatchException(String.format("Entity
			// %s references token %s, but state %s holds no reference to that",
			// e.getID(), index,
			// state.getID()));
			// }
			// }
		}
	}

	private static boolean isInSentence(BratTextBoundAnnotation ann, Tokenization tokenization) {
		return tokenization.absoluteStartOffset <= ann.getStart() && ann.getEnd() <= tokenization.absoluteEndOffset;
	}

	private static EntityAnnotation convertTextBoundAnnotation(State state, AnnotationConfig config,
			Tokenization tokenization, BratTextBoundAnnotation t)
					throws AnnotationTypeMissingException, AnnotationTextMismatchException {
		EntityType entityType = config.getEntityType(t.getRole());
		if (entityType == null)
			throw new AnnotationTypeMissingException(String.format("No entity type provided for \"%s\".", t.getRole()));
		int fromTokenIndex = findTokenForPosition(t.getStart(), tokenization, true);
		int toTokenIndex = findTokenForPosition(t.getEnd(), tokenization, false) + 1;

		EntityAnnotation entity = new EntityAnnotation(state, t.getID().id, entityType, fromTokenIndex, toTokenIndex);
		entity.setOriginalStart(t.getStart());
		entity.setOriginalEnd(t.getEnd());
		entity.setOriginalText(t.getText());

		log.debug("Text Annotation: \"%s\" -> \"%s\"", t.getText(), entity.getText());
		log.debug("-- Character span %s-%s (%s-%s) -> token span %s-%s", t.getStart(), t.getEnd(),
				t.getStart() - tokenization.absoluteStartOffset, t.getEnd() - tokenization.absoluteStartOffset,
				fromTokenIndex, toTokenIndex);
		if (!t.getText().equals(entity.getText())) {
			throw new AnnotationTextMismatchException(String.format(
					"Text representations of character-level and token-level do not match: \"%s\" != \"%s\"",
					t.getText(), entity.getText()));
		}
		return entity;
	}

	private static EntityAnnotation convertEventAnnotation(State state, AnnotationConfig config,
			Tokenization tokenization, BratEventAnnotation e)
					throws AnnotationTypeMissingException, AnnotationTextMismatchException {
		Multimap<ArgumentRole, VariableID> arguments = HashMultimap.create();
		for (Entry<String, ID<? extends BratAnnotation>> entry : e.getArguments().entrySet()) {
			arguments.put(new ArgumentRole(entry.getKey()), new VariableID(entry.getValue().id));
		}

		EntityType entityType = config.getEntityType(e.getRole());
		if (entityType == null)
			throw new AnnotationTypeMissingException(String.format("No entity type provided for \"%s\".", e.getRole()));

		int fromTokenIndex = findTokenForPosition(e.getTrigger().getStart(), tokenization, true);
		int toTokenIndex = findTokenForPosition(e.getTrigger().getEnd(), tokenization, false) + 1;
		EntityAnnotation entity = new EntityAnnotation(state, e.getID().id, entityType, arguments, fromTokenIndex,
				toTokenIndex);
		entity.setOriginalStart(e.getTrigger().getStart());
		entity.setOriginalEnd(e.getTrigger().getEnd());
		entity.setOriginalText(e.getTrigger().getText());
		log.debug("Event Annotation: \"%s\" -> \"%s\"", e.getTrigger().getText(), entity.getText());
		log.debug("-- Character span %s-%s (%s-%s) -> token span %s-%s", e.getTrigger().getStart(),
				e.getTrigger().getEnd(), e.getTrigger().getStart() - tokenization.absoluteStartOffset,
				e.getTrigger().getEnd() - tokenization.absoluteStartOffset, fromTokenIndex, toTokenIndex);
		if (!e.getTrigger().getText().equals(entity.getText())) {
			throw new AnnotationTextMismatchException(String.format(
					"Text representations of character-level and token-level do not match: \"%s\" != \"%s\"",
					e.getTrigger().getText(), entity.getText()));
		}
		return entity;
	}

	// private static void convertRelationAnnotation(State state,
	// AnnotationConfig config, BratRelationAnnotation t) {
	// Multimap<ArgumentRole, VariableID> arguments = HashMultimap.create();
	// for (Entry<String, ID<? extends BratAnnotation>> entry :
	// t.getArguments().entrySet()) {
	// arguments.put(new ArgumentRole(entry.getKey()), new
	// VariableID(entry.getValue().id));
	// }
	// EntityType entityType = config.getEntityType(t.getRole());
	// /*
	// * TODO relations are not motivated by tokens in the text and thus have
	// * no start, end and text attribute. Here, these values are filled with
	// * placeholder values
	// */
	// EntityAnnotation entity = new EntityAnnotation(state, t.getID().id,
	// entityType, arguments, -1, -1);
	// state.addMutableEntity(entity);
	// }
	//
	// private static void convertAttributeAnnotation(State state,
	// AnnotationConfig config, BratAttributeAnnotation t) {
	// }

	private static int findTokenForPosition(int documentLevelCharacterPosition, Tokenization tokenization,
			boolean findLowerBound) {
		int sentenceLevelCharacterPosition = documentLevelCharacterPosition - tokenization.absoluteStartOffset;
		return ParsingUtils.binarySpanSearch(tokenization.tokens, sentenceLevelCharacterPosition, findLowerBound);
	}

}
