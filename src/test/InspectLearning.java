package test;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.LabeledDocument;
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
import sampler.DefaultInitializer;
import sampler.ExhaustiveBoundaryExplorer;
import sampler.ExhaustiveEntityExplorer;
import sampler.RelationExplorer;
import sampling.DefaultSampler;
import sampling.Explorer;
import sampling.Initializer;
import templates.AbstractTemplate;
import templates.ContextTemplate;
import templates.MorphologicalTemplate;
import templates.RelationTemplate;
import variables.State;

public class InspectLearning {

	private static Logger log = LogManager.getFormatterLogger(InspectLearning.class.getName());

	public static void main(String[] args) {
		Corpus<? extends LabeledDocument<State, State>> corpus = null;
		AnnotationConfig config = null;
		switch (1) {
		case 0:
			DefaultCorpus<LabeledDocument<State, State>> dummyCorpus = DummyData.getDummyData();
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
		List<? extends LabeledDocument<State, State>> documents = corpus.getDocuments().subList(0, 1);

		List<AbstractTemplate<State>> templates = new ArrayList<>();
		templates.add(new RelationTemplate());
		templates.add(new MorphologicalTemplate());
		templates.add(new ContextTemplate());
		Model<State> model = new Model<>(templates);

		Scorer<State> scorer = new Scorer<>(model);

		ObjectiveFunction<State, State> objective = new DefaultObjectiveFunction();

		Initializer<State, State> initializer = new DefaultInitializer();
		List<Explorer<State>> explorers = new ArrayList<>();
		explorers.add(new ExhaustiveEntityExplorer(config));
		explorers.add(new ExhaustiveBoundaryExplorer());
		explorers.add(new RelationExplorer(20, config));
		DefaultSampler<State, State, State> sampler = new DefaultSampler<>(model, scorer, objective, initializer,
				explorers);

		Trainer<State> trainer = new Trainer<>(model, scorer);

		Learner<State> learner = new DefaultLearner<>(model, scorer, 0.1);

		trainer.train(sampler, learner, documents, 1, 10);
	}
}
