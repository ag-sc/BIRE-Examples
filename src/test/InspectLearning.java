package test;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.AnnotatedDocument;
import corpus.AnnotationConfig;
import corpus.BioNLPCorpus;
import corpus.BioNLPLoader;
import corpus.Corpus;
import corpus.DefaultCorpus;
import learning.DefaultLearner;
import learning.Learner;
import learning.Model;
import learning.ObjectiveFunction;
import learning.Scorer;
import learning.Trainer;
import objective.DefaultObjectiveFunction;
import sampler.ExhaustiveBoundarySampler;
import sampler.ExhaustiveEntitySampler;
import sampler.RelationSampler;
import sampling.AbstractSampler;
import sampling.MultiSampler;
import templates.AbstractTemplate;
import templates.ContextTemplate;
import templates.MorphologicalTemplate;
import templates.RelationTemplate;
import variables.State;

public class InspectLearning {

	private static Logger log = LogManager.getFormatterLogger(InspectLearning.class.getName());

	public static void main(String[] args) {
		Corpus<? extends AnnotatedDocument<State>> corpus = null;
		AnnotationConfig config = null;
		switch (1) {
		case 0:
			DefaultCorpus<AnnotatedDocument<State>> dummyCorpus = DummyData.getDummyData();
			config = dummyCorpus.getCorpusConfig();
			corpus = dummyCorpus;
			break;
		case 1:
			BioNLPCorpus bioNLPCorpus = BioNLPLoader.loadBioNLP2013Train(false);
			config = bioNLPCorpus.getCorpusConfig();
			corpus = bioNLPCorpus;
		default:
			break;
		}

		log.debug("Corpus:\n%s", corpus);
		List<? extends AnnotatedDocument<State>> documents = corpus.getDocuments().subList(0, 1);

		List<AbstractTemplate<State>> templates = new ArrayList<>();
		templates.add(new RelationTemplate());
		templates.add(new MorphologicalTemplate());
		templates.add(new ContextTemplate());
		Model<State> model = new Model<>(templates);

		Scorer<State> scorer = new Scorer<>(model);

		ObjectiveFunction<State> objective = new DefaultObjectiveFunction();

		List<AbstractSampler<State>> samplers = new ArrayList<>();
		samplers.add(new ExhaustiveEntitySampler(model, scorer, objective, config));
		samplers.add(new ExhaustiveBoundarySampler(model, scorer, objective));
		samplers.add(new RelationSampler(model, scorer, objective, 20));
		MultiSampler<State> sampler = new MultiSampler<>(samplers);

		Trainer<State> trainer = new Trainer<>(model, scorer, sampler);

		Learner<State> learner = new DefaultLearner<>(model, scorer, 0.1);

		trainer.train(learner, documents, 1, 10);
	}
}
