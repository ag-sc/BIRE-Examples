package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.AnnotatedDocument;
import corpus.BioNLPLoader;
import corpus.Corpus;
import evaluation.EvaluationUtil;
import factors.AbstractFactor;
import learning.Vector;
import logging.Log;
import templates.AbstractTemplate;
import templates.ContextTemplate;
import templates.MorphologicalTemplate;
import templates.RelationTemplate;
import variables.EntityAnnotation;
import variables.EntityType;
import variables.State;

public class InspectTemplates {

	private static Logger log = LogManager.getFormatterLogger(InspectTemplates.class.getName());

	public static void main(String[] args) {
		Corpus<? extends AnnotatedDocument<State, State>> corpus = null;

		switch (1) {
		case 0:
			corpus = DummyData.getDummyData();
			break;
		case 1:
			corpus = BioNLPLoader.loadBioNLP2013Train(false);
			break;
		default:
			break;
		}
		AnnotatedDocument<State, State> doc = corpus.getDocuments().get(2);
		log.debug("Content: %s (%s)", doc.getContent(), doc.getContent().length());
		log.debug("Tokens: %s", doc.getTokens());
		log.debug("State: %s", doc.getGoldResult());

		List<AbstractTemplate<State>> templates = Arrays.asList(new MorphologicalTemplate(), new ContextTemplate(),
				new RelationTemplate());

		State state = new State(doc.getGoldResult());
		applyTemplatesToState(templates, state, false);
		// state.markAsUnchanged();
		log.debug("");
		log.debug("########### Modify State ###########");
		log.debug("");
		EntityAnnotation e = new ArrayList<>(state.getEntities()).get(0);
		e.setType(new EntityType("Banana"));
		applyTemplatesToState(templates, state, false);

	}

	private static void applyTemplatesToState(List<AbstractTemplate<State>> templates, State state, boolean force) {
		log.debug("All entities:   %s", state.getEntities());
		log.debug("All entityIDs: %s", state.getEntityIDs());
		log.debug("Non-fixed entities: %s", state.getNonFixedEntities());
		log.debug("Non-fixed entityIDs: %s", state.getNonFixedEntityIDs());
		for (AbstractTemplate<State> t : templates) {
			t.applyTo(state, force);
		}

		Set<AbstractFactor> factors = state.getFactorGraph().getFactors();
		int i = 0;
		for (AbstractFactor factor : factors) {
			log.debug("\tFactor %s", factor);
			Vector v = factor.getFeatureVector();
			for (String f : v.getFeatureNames()) {
				log.debug("\t%s:\t%s", EvaluationUtil.featureWeightFormat.format(v.getValueOfFeature(f)), f);
			}
			i++;
		}
	}

}
