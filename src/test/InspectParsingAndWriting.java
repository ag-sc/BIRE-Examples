package test;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.BioNLPLoader;
import corpus.Corpus;
import corpus.FileUtils;
import corpus.SubDocument;
import evaluation.BioNLPEvaluationUtils;
import variables.State;

public class InspectParsingAndWriting {
	private static Logger log = LogManager.getFormatterLogger();

	/**
	 * Parses one specific document (annotation-text file pair) into the BIRE
	 * format. The parsed annotations (as part of the document's gold state) are
	 * then re-written to a file. The contents of the generated annotation file
	 * are supposed to be equal to the source annotation file (w.r.t. the BioNLP
	 * "strict equality" criterion). This means, the generated textual
	 * representations is not going to be identical in terms of characters, but
	 * equal, considering the portrayed entities/events.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		File goldDir = new File("/homes/sjebbara/datasets/BioNLP-ST-2013-GE/eval test/gold");
		File predDir = new File("/homes/sjebbara/datasets/BioNLP-ST-2013-GE/eval test/predicted/");

		List<File> texts = FileUtils.getFiles(goldDir, "txt");
		List<File> annotationsA1 = FileUtils.getFiles(goldDir, "a1");
		List<File> annotationsA2 = FileUtils.getFiles(goldDir, "a2");

		Corpus<SubDocument> corpus = BioNLPLoader.convertDatasetToJavaBinaries(texts, annotationsA1, annotationsA2,
				null);

		List<SubDocument> documents = corpus.getDocuments();
		List<State> states = documents.stream().map(d -> d.getGoldResult()).collect(Collectors.toList());
		log.debug("#####################");
		Set<File> files = BioNLPEvaluationUtils.statesToBioNLPFiles(predDir, states, true);
		log.debug("Parsed and written %s documents", files.size());
		// Log.d("### Original:\n%s", FileUtils.readFile(annFile));
		// Log.d("### Predicted:\n%s", annotationsAsText);

		states.forEach(s -> log.warn("%s", s));
		// Log.d("#####################");
		// State state1 = documents.get(4).getGoldState();
		// State state2 = new State(state1);
		// MutableEntityAnnotation removedEntity = new
		// ArrayList<>(state2.getMutableEntities()).get(0);
		// state2.removeMutableEntity(removedEntity.getID());
		// Log.d("State 2: remove entity %s", removedEntity);
		// Log.d(state1.getDocument().getContent());
		// Log.d("State 1: %s", state1);
		// Log.d("State 2: %s", state2);
		// Log.d("F1(State 1, State 1) = %s",
		// BioNLPEvaluation.strictEquality(state1, state1));
		// Log.d("F1(State 2, State 2) = %s",
		// BioNLPEvaluation.strictEquality(state2, state2));
		// Log.d("F1(State 1, State 2) = %s",
		// BioNLPEvaluation.strictEquality(state1, state2));
		// Log.d("F1(State 2, State 1) = %s",
		// BioNLPEvaluation.strictEquality(state2, state1));
	}

}
