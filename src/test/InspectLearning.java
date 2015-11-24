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
import learning.DefaultLearner;
import learning.Learner;
import learning.Model;
import templates.AbstractTemplate;
import templates.MorphologicalTemplate;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.EntityType;
import variables.State;

public class InspectLearning {

	private static Logger log = LogManager.getFormatterLogger(InspectLearning.class.getName());

	public static void main(String[] args) {

		MorphologicalTemplate template = new MorphologicalTemplate();
		List<AbstractTemplate<State>> templates = new ArrayList<>();
		templates.add(template);
		Model<State> model = new Model<>(templates);

		String content = "This is a test";
		List<Token> tokens = Arrays.asList(new Token(0, 0, 4, "This"), new Token(1, 5, 7, "is"),
				new Token(2, 8, 9, "a"), new Token(3, 10, 14, "test"));
		Document<State> doc = new Document<>("test", content, tokens);
		State s1 = new State(doc);
		EntityAnnotation e1 = new EntityAnnotation(s1, "E1", new EntityType("Banana"), 0, 1);
		EntityAnnotation e2 = new EntityAnnotation(s1, "E2", new EntityType("Apple"), 2, 3);
		EntityAnnotation e3 = new EntityAnnotation(s1, "E3", new EntityType("Pear"), 3, 4);
		s1.addEntity(e1);
		s1.addEntity(e2);
		s1.setObjectiveScore(0.5);

		State s2 = new State(doc);
		s2.addEntity(e2);
		s2.addEntity(e3);
		s2.setObjectiveScore(0.8);

		log.debug("S1: %s", s1);
		log.debug("S2: %s", s2);

		template.applyTo(s1, true);
		template.applyTo(s2, true);

		log.debug("Factors S1: %s", s1.getFactorGraph().getFactors());
		log.debug("Factors S2: %s", s2.getFactorGraph().getFactors());

		log.debug("Model: %s", model.toDetailedString());
		Learner<State> learner = new DefaultLearner<>(model, 1);
		learner.update(s1, s2);

		log.debug("Model: %s", model.toDetailedString());
	}

}
