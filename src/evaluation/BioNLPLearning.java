package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.AnnotatedDocument;
import corpus.BioNLPCorpus;
import corpus.BioNLPLoader;
import corpus.SubDocument;
import learning.DefaultLearner;
import learning.Model;
import learning.ObjectiveFunction;
import learning.Scorer;
import learning.Trainer;
import objective.BetterObjectiveFunction;
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

public class BioNLPLearning {
	private static Logger log = LogManager.getFormatterLogger();

	public static void main(String[] args) {
		// int trainSize = 190;

		int trainSize = 1;
		int testSize = 1;
		if (args != null && args.length == 2) {
			trainSize = Integer.parseInt(args[0]);
			testSize = Integer.parseInt(args[1]);
		}
		BioNLPCorpus trainCorpus = BioNLPLoader.loadBioNLP2013Train(false);
		BioNLPCorpus devCorpus = BioNLPLoader.loadBioNLP2013Dev(false);

		log.debug("Train corpus config: %s", trainCorpus.getCorpusConfig());
		log.debug("Dev corpus config: %s", devCorpus.getCorpusConfig());

		File modelDir = new File("res/bionlp/models");
		File evalDir = new File("res/bionlp/eval");
		File outputDir = new File("res/bionlp/gen");
		File predictionFile = new File("res/bionlp/prediction.bin");

		if (!modelDir.exists())
			modelDir.mkdirs();

		if (!evalDir.exists())
			evalDir.mkdirs();

		if (!outputDir.exists())
			outputDir.mkdirs();

		int numberOfSamplingSteps = 10;
		int numberOfEpochs = 1;
		// N-Fold cross validation
		int n = 1;
		// long[] seeds = { 1234, 2345, 3456 };
		List<State> predictions = null;
		for (int i = 0; i < n; i++) {
			log.info("############################");
			log.info("############################");
			log.info("Cross Validation: %s/%s", i + 1, n);
			List<SubDocument> train = null;
			List<SubDocument> test = null;
			if (trainSize > 0) {
				trainSize = Math.min(trainSize, trainCorpus.getParentDocuments().size());
				train = trainCorpus.getSubDocuments(trainCorpus.getParentDocuments().subList(0, trainSize));
			} else {
				train = trainCorpus.getDocuments();
			}
			if (testSize > 0) {
				testSize = Math.min(testSize, devCorpus.getParentDocuments().size());
				test = devCorpus.getSubDocuments(devCorpus.getParentDocuments().subList(0, testSize));
			} else {
				test = devCorpus.getDocuments();
			}
			log.info("Train/test: => #train: %s, #test: %s", train.size(), test.size());

			ObjectiveFunction<State, State> objective = new BetterObjectiveFunction();
			List<AbstractTemplate<State>> templates = new ArrayList<>();
			templates.add(new RelationTemplate());
			templates.add(new MorphologicalTemplate());
			templates.add(new ContextTemplate());
			// templates.add(new ObjectiveFunctionTemplate(objective));
			Model<State> model = new Model<>(templates);

			Scorer<State> scorer = new Scorer<>(model);

			Initializer<State, State> initializer = new DefaultInitializer();
			List<Explorer<State>> explorer = new ArrayList<>();
			explorer.add(new ExhaustiveEntityExplorer(trainCorpus.getCorpusConfig()));
			explorer.add(new ExhaustiveBoundaryExplorer());
			explorer.add(new RelationExplorer(20));
			DefaultSampler<State, State, State> sampler = new DefaultSampler<>(model, scorer, objective, initializer,
					explorer);

			Trainer<State> trainer = new Trainer<>(model, scorer);

			DefaultLearner<State> learner = new DefaultLearner<>(model, scorer, 1);

			log.info("####################");
			log.info("Start training");
			trainer.train(sampler, learner, train, numberOfEpochs, numberOfSamplingSteps);
			log.info("###############");
			log.info("Trained Model:\n%s", model.toDetailedString());
			log.info("###############");
			try {
				model.saveModelToFile(
						new File(modelDir, EvaluationUtil.generateFilenameForModel(train.size())).getPath());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			predictions = trainer.test(sampler, test, numberOfSamplingSteps);
			Set<File> files = BioNLPEvaluation.statesToBioNLPFiles(outputDir, predictions, true);
			log.info("Produced annotaion files: %s", files);

			log.info("Updates: %s, Alpha: %s", learner.updates, learner.currentAlpha);
		}
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(predictionFile));
			out.writeObject(predictions);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		BetterObjectiveFunction o = new BetterObjectiveFunction();
		for (State state : predictions) {
			State goldState = ((AnnotatedDocument<State, State>) state.getDocument()).getGoldResult();
			double s = o.score(state, goldState);
			// if (s < 0.9) {
			// log.info("Gold: : %s", goldState);
			// log.info("Prediction: %s\n", state);
			// }
		}
		log.info("Overall performance:");
		EvaluationUtil.printPredictionPerformance(predictions);
		TaggedTimer.printTimings();

	}
}
