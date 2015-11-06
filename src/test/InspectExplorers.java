package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import corpus.Document;
import corpus.Token;
import sampler.ExhaustiveBoundaryExplorer;
import sampling.Explorer;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.EntityType;
import variables.State;

public class InspectExplorers {

	private static Logger log = LogManager.getFormatterLogger(InspectExplorers.class.getName());

	public static void main(String[] args) {
		testMerge();
		// Corpus<? extends AnnotatedDocument<State, State>> corpus = null;
		// AnnotationConfig corpusConfig = null;
		//
		// switch (1) {
		// case 0:
		// DefaultCorpus<? extends AnnotatedDocument<State, State>> c1 =
		// DummyData.getDummyData();
		// corpusConfig = c1.getCorpusConfig();
		// corpus = c1;
		// break;
		// case 1:
		// BioNLPCorpus c2 = BioNLPLoader.loadBioNLP2013Train(false);
		// corpusConfig = c2.getCorpusConfig();
		// corpus = c2;
		// break;
		// default:
		// break;
		// }
		// AnnotatedDocument<State, State> doc = corpus.getDocuments().get(0);
		// log.debug("Content: %s (%s)", doc.getContent(),
		// doc.getContent().length());
		// log.debug("Tokens: %s", doc.getTokens());
		// log.debug("State: %s", doc.getGoldResult());
		//
		// Initializer<State, State> initializer = new DefaultInitializer();
		// List<Explorer<State>> explorers = new ArrayList<>();
		// explorers.add(new ExhaustiveEntityExplorer(corpusConfig));
		// explorers.add(new ExhaustiveBoundaryExplorer());
		// explorers.add(new RelationExplorer(10));
		//
		//
		// State init = doc.getPriorKnowledge();
		// log.debug("################################");
		// log.debug("%s", init);
		// State gold = doc.getGoldResult();
		// log.debug("%s", gold);
		// log.debug("################################");
		//
		// State state = initializer.getInitialState(doc);
		// applyExplorersToState(explorers, state, false);
	}

	private static void testMerge() {
		String content = "This is a test";
		List<Token> tokens = Arrays.asList(new Token(0, 0, 4, "This"), new Token(1, 5, 7, "is"),
				new Token(2, 8, 9, "a"), new Token(3, 10, 14, "test"));
		Document<State> doc = new Document<>("test", content, tokens);
		State s = new State(doc);
		Multimap<ArgumentRole, VariableID> args1 = HashMultimap.create();
		args1.put(new ArgumentRole("Child"), new VariableID("E3"));
		Multimap<ArgumentRole, VariableID> args2 = HashMultimap.create();
		args2.put(new ArgumentRole("Child"), new VariableID("E3"));
		EntityAnnotation e1 = new EntityAnnotation(s, "E1", new EntityType("BANANA"), args1, 0, 1);
		EntityAnnotation e2 = new EntityAnnotation(s, "E2", new EntityType("BANANA"), args2, 1, 3);
		EntityAnnotation e3 = new EntityAnnotation(s, "E3", new EntityType("BANANA"), 3, 4);
		s.addEntity(e1);
		s.addEntity(e2);
		s.addEntity(e3);

		List<Explorer<State>> explorers = new ArrayList<>();
		// explorers.add(new ExhaustiveEntityExplorer(corpusConfig));
		explorers.add(new ExhaustiveBoundaryExplorer());
		// explorers.add(new RelationExplorer(10));

		applyExplorersToState(explorers, s, false);
	}

	private static void applyExplorersToState(List<Explorer<State>> explorers, State state, boolean force) {
		log.debug("All entities:   %s", state.getEntities());
		log.debug("All entityIDs: %s", state.getEntityIDs());
		log.debug("Non-fixed entities: %s", state.getNonFixedEntities());
		log.debug("Non-fixed entityIDs: %s", state.getNonFixedEntityIDs());
		for (Explorer<State> ex : explorers) {
			log.debug("----- %s -----", ex.getClass().getSimpleName());
			log.debug("INITIAL STATE: %s", state);
			log.debug("------");
			ex.getNextStates(state).forEach(s -> log.debug("%s", s));
		}
	}

}
