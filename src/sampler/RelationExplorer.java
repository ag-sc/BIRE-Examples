package sampler;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import changes.StateChange;
import corpus.AnnotationConfig;
import logging.Log;
import sampling.Explorer;
import variables.EntityAnnotation;
import variables.State;

public class RelationExplorer implements Explorer<State> {

	private static Logger log = LogManager.getFormatterLogger(RelationExplorer.class.getName());

	private int numberOfStates;
	private AnnotationConfig corpusConfig;

	public RelationExplorer(int numberOfStates, AnnotationConfig corpusConfig) {
		this.numberOfStates = numberOfStates;
		this.corpusConfig = corpusConfig;
	}

	public List<State> getNextStates(State previousState) {
		List<State> nextStates = new ArrayList<State>();
		for (int i = 0; i < numberOfStates; i++) {
			State generatedState = new State(previousState);

			// pick one entity at random
			EntityAnnotation sampledEntity = SamplingHelper
					.getRandomElement(new ArrayList<>(generatedState.getNonFixedEntities()));
			// if annotation exists
			if (sampledEntity != null) {
				// choose a way to alter the state
				StateChange stateChange = SamplingHelper.sampleStateChange(StateChange.ADD_ARGUMENT,
						StateChange.REMOVE_ARGUMENT, StateChange.CHANGE_ARGUMENT_ROLE,
						StateChange.CHANGE_ARGUMENT_ENTITY, StateChange.DO_NOTHING);
				switch (stateChange) {
				case ADD_ARGUMENT:
					Log.d("%s: add annotation argument.", generatedState.getID());
					SamplingHelper.addRandomArgument(sampledEntity, generatedState, corpusConfig);
					break;
				case REMOVE_ARGUMENT:
					Log.d("%s: remove annotation argument.", generatedState.getID());
					SamplingHelper.removeRandomArgument(sampledEntity);
					break;
				case CHANGE_ARGUMENT_ROLE:
					Log.d("%s: change argument role", generatedState.getID());
					SamplingHelper.changeRandomArgumentRole(sampledEntity, generatedState, corpusConfig);
					break;
				case CHANGE_ARGUMENT_ENTITY:
					Log.d("%s: change argument entity", generatedState.getID());
					SamplingHelper.changeRandomArgumentEntity(sampledEntity, generatedState, corpusConfig);
					break;
				case DO_NOTHING:
					Log.d("Do not change the state");
					break;
				default:
					Log.d("%s: unsupported state change", generatedState.getID());
					break;
				}
			}
			nextStates.add(generatedState);
		}
		return nextStates;

	}

}
