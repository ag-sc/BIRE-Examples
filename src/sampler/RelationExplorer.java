package sampler;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import changes.StateChange;
import logging.Log;
import sampling.Explorer;
import variables.MutableEntityAnnotation;
import variables.State;

public class RelationExplorer implements Explorer<State> {

	private static Logger log = LogManager.getFormatterLogger(RelationExplorer.class.getName());

	private int numberOfStates;

	public RelationExplorer(int numberOfStates) {
		this.numberOfStates = numberOfStates;
	}

	public List<State> getNextStates(State previousState) {
		List<State> nextStates = new ArrayList<State>();
		for (int i = 0; i < numberOfStates; i++) {
			State generatedState = new State(previousState);

			// pick one entity at random
			MutableEntityAnnotation sampledEntity = SamplingHelper
					.getRandomElement(new ArrayList<>(generatedState.getMutableEntities()));
			// if annotation exists
			if (sampledEntity != null) {
				// choose a way to alter the state
				StateChange stateChange = SamplingHelper.sampleStateChange(StateChange.ADD_ARGUMENT,
						StateChange.REMOVE_ARGUMENT, StateChange.CHANGE_ARGUMENT_ROLE,
						StateChange.CHANGE_ARGUMENT_ENTITY, StateChange.DO_NOTHING);
				switch (stateChange) {
				case ADD_ARGUMENT:
					Log.d("%s: add annotation argument.", generatedState.getID());
					SamplingHelper.addRandomArgument(sampledEntity, generatedState);
					break;
				case REMOVE_ARGUMENT:
					Log.d("%s: remove annotation argument.", generatedState.getID());
					SamplingHelper.removeRandomArgument(sampledEntity);
					break;
				case CHANGE_ARGUMENT_ROLE:
					Log.d("%s: change argument role", generatedState.getID());
					SamplingHelper.changeRandomArgumentRole(sampledEntity, generatedState);
					break;
				case CHANGE_ARGUMENT_ENTITY:
					Log.d("%s: change argument entity", generatedState.getID());
					SamplingHelper.changeRandomArgumentEntity(sampledEntity, generatedState);
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
