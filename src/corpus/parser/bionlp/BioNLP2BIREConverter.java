package corpus.parser.bionlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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
import logging.Log;
import utility.ID;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.EntityType;
import variables.State;

public class BioNLP2BIREConverter {

	private static Logger log = LogManager.getFormatterLogger(BioNLP2BIREConverter.class.getName());

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
	public static List<SubDocument> convert(BratAnnotatedDocument bratDoc, List<Tokenization> tokenizations)
			throws AnnotationFileException {
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

		Set<ID<BratTextBoundAnnotation>> triggerIDs = new HashSet<>();
		for (BratEventAnnotation event : eventAnnotationsA2) {
			triggerIDs.add(event.getTriggerID());
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
			 * initial knowledge.
			 */
			log.debug("Parse A1 annotations...");
			if (textAnnotationsA1.isEmpty()) {
				log.warn("The SubDocument %s has no A1 annotations", doc.getName());
			}
			for (BratTextBoundAnnotation tann : textAnnotationsA1) {
				if (isInSentence(tann, tokenization)) {
					try {
						EntityAnnotation entity = convertTextBoundAnnotation(priorKnowledge, tokenization, tann);
						entity.setPriorKnowledge(true);
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
				/*
				 * Only add text bound annotations that are not the trigger of
				 * an event.
				 */
				if (!triggerIDs.contains(tann.getID())) {
					if (isInSentence(tann, tokenization)) {
						try {
							EntityAnnotation entity = convertTextBoundAnnotation(goldState, tokenization, tann);
							goldState.addEntity(entity);
						} catch (Exception e) {
							log.warn(e);
						}
					}
				}
			}

			for (BratEventAnnotation eann : eventAnnotationsA2) {
				if (isInSentence(eann.getTrigger(), tokenization)) {
					try {
						EntityAnnotation entity = convertEventAnnotation(goldState, tokenization, eann);
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
		Collections.sort(textAnnotations, (a1, a2) -> a1.getStart() - a2.getStart());
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

	private static EntityAnnotation convertTextBoundAnnotation(State state, Tokenization tokenization,
			BratTextBoundAnnotation t) throws AnnotationTypeMissingException, AnnotationTextMismatchException {
		String role = t.getRole();
		if (role == null)
			throw new AnnotationTypeMissingException(String.format("No entity type provided for \"%s\".", role));
		EntityType entityType = new EntityType(role);

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

	private static EntityAnnotation convertEventAnnotation(State state, Tokenization tokenization,
			BratEventAnnotation e) throws AnnotationTypeMissingException, AnnotationTextMismatchException {
		Multimap<ArgumentRole, VariableID> arguments = HashMultimap.create();
		for (Entry<String, ID<? extends BratAnnotation>> entry : e.getArguments().entries()) {
			arguments.put(new ArgumentRole(entry.getKey()), new VariableID(entry.getValue().id));
		}

		String role = e.getRole();
		if (role == null)
			throw new AnnotationTypeMissingException(String.format("No entity type provided for \"%s\".", role));
		EntityType entityType = new EntityType(role);

		BratTextBoundAnnotation triggerAnnotation = e.getTrigger();
		/*
		 * Remove the trigger annotation since it is merged into the event.
		 */
		// state.removeEntity(new VariableID(triggerAnnotation.getID().id));

		int fromTokenIndex = findTokenForPosition(triggerAnnotation.getStart(), tokenization, true);
		int toTokenIndex = findTokenForPosition(triggerAnnotation.getEnd(), tokenization, false) + 1;
		EntityAnnotation entity = new EntityAnnotation(state, e.getID().id, entityType, arguments, fromTokenIndex,
				toTokenIndex);
		entity.setOriginalStart(triggerAnnotation.getStart());
		entity.setOriginalEnd(triggerAnnotation.getEnd());
		entity.setOriginalText(triggerAnnotation.getText());
		log.debug("Event Annotation: \"%s\" -> \"%s\"", triggerAnnotation.getText(), entity.getText());
		log.debug("-- Character span %s-%s (%s-%s) -> token span %s-%s", triggerAnnotation.getStart(),
				triggerAnnotation.getEnd(), triggerAnnotation.getStart() - tokenization.absoluteStartOffset,
				triggerAnnotation.getEnd() - tokenization.absoluteStartOffset, fromTokenIndex, toTokenIndex);
		if (!triggerAnnotation.getText().equals(entity.getText())) {
			throw new AnnotationTextMismatchException(String.format(
					"Text representations of character-level and token-level do not match: \"%s\" != \"%s\"",
					triggerAnnotation.getText(), entity.getText()));
		}
		return entity;
	}

	private static int findTokenForPosition(int documentLevelCharacterPosition, Tokenization tokenization,
			boolean findLowerBound) {
		int sentenceLevelCharacterPosition = documentLevelCharacterPosition - tokenization.absoluteStartOffset;
		return ParsingUtils.binarySpanSearch(tokenization.tokens, sentenceLevelCharacterPosition, findLowerBound);
	}

}
