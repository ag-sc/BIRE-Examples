package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.AnnotationConfig;
import corpus.BioNLPCorpus;
import corpus.BioNLPLoader;
import corpus.LabeledDocument;
import corpus.SubDocument;
import corpus.TFIDFStore;
import learning.DefaultLearner;
import learning.Model;
import learning.ObjectiveFunction;
import learning.Scorer;
import learning.Trainer;
import objective.DefaultObjectiveFunction;
import sampler.DefaultInitializer;
import sampler.ExhaustiveBoundaryExplorer;
import sampler.ExhaustiveEntityExplorer;
import sampling.DefaultSampler;
import sampling.Explorer;
import sampling.Initializer;
import sampling.JoinExplorer;
import templates.AbstractTemplate;
import templates.MorphologicalTemplate;
import templates.TFIDFTemplate;
import variables.State;

public class BioNLPLearning {
	private static Logger log = LogManager.getFormatterLogger();

	public static void main(String[] args) {
		// int trainSize = 190;

		int trainSize = 30;
		int testSize = 30;
		if (args != null && args.length == 2) {
			trainSize = Integer.parseInt(args[0]);
			testSize = Integer.parseInt(args[1]);
		}
		BioNLPCorpus trainCorpus = BioNLPLoader.loadBioNLP2013Train(false);
		BioNLPCorpus devCorpus = BioNLPLoader.loadBioNLP2013Dev(false);

		AnnotationConfig corpusConfig = new AnnotationConfig();
		corpusConfig.addEntityType(trainCorpus.getCorpusConfig().getEntityTypeDefinition("Protein"));

		log.debug("Train corpus config: %s", corpusConfig);

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

		int numberOfSamplingSteps = 20;
		int numberOfEpochs = 10;
		// N-Fold cross validation
		int n = 1;
		// long[] seeds = { 1234, 2345, 3456 };

		// for (int i = 0; i < n; i++) {
		log.info("############################");
		log.info("############################");
		// log.info("Cross Validation: %s/%s", i + 1, n);
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
		log.info("####### remove events for simplification ########");
		for (SubDocument document : train) {
			document.setGoldResult(document.getPriorKnowledge());
			document.setPriorKnowledge(new State(document));
			// log.info(document.getGoldResult());
			// log.info(document.getPriorKnowledge());
		}
		for (SubDocument document : test) {
			document.setGoldResult(document.getPriorKnowledge());
			document.setPriorKnowledge(new State(document));
		}

		log.info("####### remove empty states for simplification ########");
		for (Iterator iterator = train.iterator(); iterator.hasNext();) {
			SubDocument document = (SubDocument) iterator.next();
			if (document.getGoldResult().getEntities().isEmpty()) {
				iterator.remove();
			}
		}
		for (Iterator iterator = test.iterator(); iterator.hasNext();) {
			SubDocument document = (SubDocument) iterator.next();
			if (document.getGoldResult().getEntities().isEmpty()) {
				iterator.remove();
			}
		}
		log.info("Train/test: => #train: %s, #test: %s", train.size(), test.size());

		TFIDFStore store = new TFIDFStore(train);
		System.out.println("ROWS:");
		for (String term : store.tfTable.rowKeySet()) {
			log.debug("");
			log.debug("---------- " + term + " -----------");
			String tmp = "";
			for (String entityType : store.tfTable.columnKeySet()) {
				tmp += String.format("%s: %f\t", entityType, store.getTFIDF(term, entityType));
			}
			log.debug(tmp);
			log.debug("");
		}

		ObjectiveFunction<State, State> objective = new DefaultObjectiveFunction(true);
		List<AbstractTemplate<State>> templates = new ArrayList<>();
		templates.add(new TFIDFTemplate(store));
		templates.add(new MorphologicalTemplate());
		// templates.add(new ContextTemplate());
		// templates.add(new EntityTemplate());
		// templates.add(new MetaTemplate());
		// templates.add(new RelationTemplate());
		Model<State> model = new Model<>(templates);

		Scorer<State> scorer = new Scorer<>(model);

		Initializer<SubDocument, State> initializer = new DefaultInitializer<>(false);

		List<Explorer<State>> explorers = new ArrayList<>();
		explorers.add(new ExhaustiveEntityExplorer(corpusConfig));
		explorers.add(new ExhaustiveBoundaryExplorer(true, true));
		// explorers.add(new
		// ExhaustiveRelationExplorer(trainCorpus.getCorpusConfig()));
		DefaultSampler<State, State> sampler = new DefaultSampler<>(model, scorer, objective,
				Arrays.asList(new JoinExplorer<>(explorers)), numberOfSamplingSteps);

		Trainer trainer = new Trainer();
		DefaultLearner<State> learner = new DefaultLearner<>(model, 1);

		trainer.addInstanceCallback(learner);
		// trainer.addEpochCallback(sampler);

		log.info("####################");
		log.info("Start training");

		trainer.train(sampler, initializer, learner, train, numberOfEpochs);
		try {
			model.saveModelToFile(new File(modelDir, EvaluationUtil.generateFilenameForModel(train.size())).getPath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// sampler.setStepLimit(15);
		// TODO use test here
		List<State> predictions = trainer.test(sampler, initializer, test);
		log.info("###############");
		log.info("Trained Model Weights:");
		EvaluationUtil.printWeights(model, -1);
		log.info("###############");
		Set<File> files = BioNLPEvaluationUtils.statesToBioNLPFiles(outputDir, predictions, true);
		log.info("Produced annotaion files: %s", files);

		log.info("Updates: %s, Alpha: %s", learner.updates, learner.currentAlpha);
		// }

		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(predictionFile));
			out.writeObject(predictions);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (State state : predictions) {
			State goldState = ((LabeledDocument<State, State>) state.getDocument()).getGoldResult();
			double s = objective.score(state, goldState);
		}
		log.info("Overall performance:");
		EvaluationUtil.printPredictionPerformance(predictions);

		F1Score entityOnlyScore = BioNLPEvaluationUtils.evaluatePrediction(predictions, true, true, false);
		F1Score entityAndTriggerOnlyScore = BioNLPEvaluationUtils.evaluatePrediction(predictions, true, false, false);
		F1Score relationScore = BioNLPEvaluationUtils.evaluatePrediction(predictions, false, false, true);
		F1Score overallScore = BioNLPEvaluationUtils.evaluatePrediction(predictions, false, false, false);
		log.info("Score entities:           %s", entityOnlyScore);
		log.info("Score entities & trigger: %s", entityAndTriggerOnlyScore);
		log.info("Score no prior:           %s", relationScore);
		log.info("Score all:                %s", overallScore);

		TaggedTimer.printTimings();

	}
}
