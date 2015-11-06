package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import corpus.Document;
import sampling.DefaultSampler;
import variables.State;

public class TestScript {
	public static void main(String[] args) throws FileNotFoundException, ClassNotFoundException, IOException {
		// File predictionFile = new File("res/bionlp/prediction.bin");
		// List<State> predictions = loadPredictions(predictionFile);
		// DefaultObjectiveFunction o = new DefaultObjectiveFunction();
		// for (State state : predictions) {
		// State goldState = ((AnnotatedDocument<State, State>)
		// state.getDocument()).getGoldResult();
		// o.score(state, goldState);
		// }
		// EvaluationUtil.printPredictionPerformance(predictions);

		List<State> nextStates = new ArrayList<>();
		Document d = null;
		for (int i = 0; i < 20; i++) {
			State s = new State(d);
			s.setModelScore(Math.random() * 1);
			s.setObjectiveScore(Math.random());
			nextStates.add(s);
			// System.out.println(String.format("%s: %s", s.getID(),
			// s.getModelScore()));
		}
		Function<Double, Double> toProbability = s ->s;
		Function<Double, Double> toSoftmax = s -> Math.exp(s);
		Collections.sort(nextStates, State.modelScoreComparator);
		double totalSum = 0;
		for (State s : nextStates) {
			totalSum += toProbability.apply(s.getModelScore());
		}
		System.out.println(totalSum);
		for (State s : nextStates) {
			System.out.println(String.format("%s: %s,\t%s,\t%s", s.getID(), s.getModelScore(),
					toProbability.apply(s.getModelScore()), toProbability.apply(s.getModelScore()) / totalSum));
		}
		// System.out.println("------------------");
		// for (int i = 0; i < 100; i++) {
		// State s = DefaultSampler.drawRandomlyFrom(nextStates, false, true);
		// System.out.println(String.format("%s: %s, %s", s.getID(),
		// s.getModelScore(), s.getObjectiveScore()));
		// }
	}

	private static List<State> loadPredictions(File predictionFile)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(predictionFile));
		try {
			List<State> predictions = (List<State>) in.readObject();
			return predictions;
		} finally {
			in.close();
		}
	}
}
