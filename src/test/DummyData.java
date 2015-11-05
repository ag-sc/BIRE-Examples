package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import corpus.AnnotatedDocument;
import corpus.AnnotationConfig;
import corpus.BioNLPLoader;
import corpus.DefaultCorpus;
import corpus.Token;
import corpus.parser.bionlp.BratConfigReader;
import logging.Log;
import variables.EntityAnnotation;
import variables.State;

public class DummyData {

	public static DefaultCorpus<AnnotatedDocument<State, State>> getDummyData() {
		BratConfigReader configReader = new BratConfigReader();
		AnnotationConfig originalConfig = configReader.readConfig(new File("res/bionlp/annotation.conf"));
		AnnotationConfig simplifiedConfig = new AnnotationConfig();
		simplifiedConfig.addEntityType(originalConfig.getEntityType("Protein"));

		String content = "a critical role for tumor necrosis factor and interleukin-7";
		List<Token> tokens = extractTokens(content);
		Log.d("Tokens for dummy data: %s", tokens);

		DefaultCorpus<AnnotatedDocument<State, State>> corpus = new DefaultCorpus<>(simplifiedConfig);
		AnnotatedDocument<State, State> doc = new AnnotatedDocument<State, State>("DummyDocument", content, tokens);
		State goldState = new State(doc);
		doc.setGoldResult(goldState);

		EntityAnnotation e1 = new EntityAnnotation(goldState, "T1", simplifiedConfig.getEntityType("Protein"), 4, 6);
		goldState.addEntity(e1);
		EntityAnnotation e2 = new EntityAnnotation(goldState, "T2", simplifiedConfig.getEntityType("Protein"), 8, 8);
		goldState.addEntity(e2);

		corpus.addDocument(doc);

		return corpus;
	}

	public static AnnotatedDocument<State, State> getRepresentativeDummyData()
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
