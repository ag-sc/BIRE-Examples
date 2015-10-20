package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
import variables.EntityType;
import variables.MutableEntityAnnotation;
import variables.State;

public class InspectTemplates {

	public static void main(String[] args) {
		Corpus<? extends AnnotatedDocument<State>> corpus = null;

		switch (1) {
		case 0:
			corpus = DummyData.getDummyData();
			break;
		case 1:
			corpus = BioNLPLoader.loadBioNLP2013Train(true);
			break;
		default:
			break;
		}
		AnnotatedDocument<State> doc = corpus.getDocuments().get(1);
		Log.d("Content: %s (%s)", doc.getContent(), doc.getContent().length());
		Log.d("Tokens: %s", doc.getTokens());
		Log.d("State: %s", doc.getGoldState());

		List<AbstractTemplate<State>> templates = Arrays.asList(new MorphologicalTemplate(), new ContextTemplate(),
				new RelationTemplate());

		State state = doc.getGoldState().duplicate();
		applyTemplatesToState(templates, state, false);
		state.markAsUnchanged();
		Log.d("");
		Log.d("########### Modify State ###########");
		Log.d("");
		MutableEntityAnnotation e = new ArrayList<>(state.getMutableEntities()).get(0);
		e.setType(new EntityType("Banana"));
		applyTemplatesToState(templates, state, false);

	}

	private static void applyTemplatesToState(List<AbstractTemplate<State>> templates, State state, boolean force) {
		Log.d("Mutables:   %s", state.getMutableEntities());
		Log.d("Immutables: %s", state.getImmutableEntities());
		for (AbstractTemplate<State> t : templates) {
			t.applyTo(state, force);
		}

		Set<AbstractFactor> factors = state.getFactorGraph().getFactors();
		int i = 0;
		for (AbstractFactor factor : factors) {
			Log.d("\tFactor %s", factor);
			Vector v = factor.getFeatureVector();
			for (String f : v.getFeatureNames()) {
				Log.d("\t%s:\t%s", EvaluationUtil.featureWeightFormat.format(v.getValueOfFeature(f)), f);
			}
			i++;
		}
	}

}
