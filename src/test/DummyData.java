package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.AnnotationConfig;
import corpus.BioNLPLoader;
import corpus.DefaultCorpus;
import corpus.LabeledDocument;
import corpus.Token;
import corpus.parser.bionlp.BratConfigReader;
import variables.EntityAnnotation;
import variables.EntityType;
import variables.State;

public class DummyData {
	private static Logger log = LogManager.getFormatterLogger();

	public static DefaultCorpus<LabeledDocument<State, State>> getDummyData() {
		BratConfigReader configReader = new BratConfigReader();
		AnnotationConfig originalConfig = configReader.readConfig(new File("res/bionlp/annotation.conf"));
		AnnotationConfig simplifiedConfig = new AnnotationConfig();
		simplifiedConfig.addEntityType(originalConfig.getEntityTypeDefinition("Protein"));

		String content = "a critical role for tumor necrosis factor and interleukin-7";
		List<Token> tokens = extractTokens(content);
		log.debug("Tokens for dummy data: %s", tokens);

		DefaultCorpus<LabeledDocument<State, State>> corpus = new DefaultCorpus<>(simplifiedConfig);
		LabeledDocument<State, State> doc = new LabeledDocument<State, State>("DummyDocument", content, tokens);
		State goldState = new State(doc);
		doc.setGoldResult(goldState);

		EntityAnnotation e1 = new EntityAnnotation(goldState, "T1", new EntityType("Protein"), 4, 6);
		goldState.addEntity(e1);
		EntityAnnotation e2 = new EntityAnnotation(goldState, "T2", new EntityType("Protein"), 8, 8);
		goldState.addEntity(e2);

		corpus.addDocument(doc);

		return corpus;
	}

	public static LabeledDocument<State, State> getRepresentativeDummyData()
			throws FileNotFoundException, ClassNotFoundException, IOException {
		String filename = "PMID-9119025";
		File annFile = new File("res/bionlp/ann/" + filename + ".ann");
		File textFile = new File("res/bionlp/text/" + filename + ".txt");
		return BioNLPLoader.loadDocument(textFile, Arrays.asList(annFile)).getDocuments().get(4);
	}

	private static List<Token> extractTokens(String content) {
		List<Token> tokens = new ArrayList<Token>();
		Matcher matcher = Pattern.compile("\\S+").matcher(content);

		int index = 0;
		while (matcher.find()) {
			String text = matcher.group();
			int from = matcher.start();
			int to = matcher.end();

			Token token = new Token(index, from, to, text);
			tokens.add(token);
			index++;
		}
		return tokens;
	}
}
