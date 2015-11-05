package sampler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sampling.Explorer;
import utility.VariableID;
import variables.EntityAnnotation;
import variables.State;

public class ExhaustiveBoundaryExplorer implements Explorer<State> {

	private static Logger log = LogManager.getFormatterLogger(ExhaustiveBoundaryExplorer.class.getName());

	private boolean mergeNeighbors = true;

	public List<State> getNextStates(State previousState) {
		List<State> generatedStates = new ArrayList<>();
		// merge neighboring/overlapping entities
		generatedStates.addAll(generateStatesForBoundaries(previousState));
		if (mergeNeighbors) {
			generatedStates.addAll(generateStatesForNeighbors(previousState));
		}
		return generatedStates;

	}

	private List<State> generateStatesForBoundaries(State previousState) {
		List<State> generatedStates = new ArrayList<State>();
		Set<VariableID> entities = previousState.getNonFixedEntityIDs();
		for (VariableID entityID : entities) {
			EntityAnnotation previousStatesEntity = previousState.getEntity(entityID);
			int from = previousStatesEntity.getBeginTokenIndex();
			int to = previousStatesEntity.getEndTokenIndex();
			if (0 < from) {
				// Expand left
				State generatedState = new State(previousState);
				EntityAnnotation entity = generatedState.getEntity(entityID);
				entity.setBeginTokenIndex(from - 1);
				generatedStates.add(generatedState);
			}
			if (to < previousStatesEntity.getState().getDocument().getTokens().size()) {
				// Expand right
				State generatedState = new State(previousState);
				EntityAnnotation entity = generatedState.getEntity(entityID);
				entity.setEndTokenIndex(to + 1);
				generatedStates.add(generatedState);
			}
			if (to - from > 1) {
				/**
				 * Here we just assume that to >= from. That is why we do not
				 * check if to > 0 or from < "max"-1. Given a consistent state,
				 * these conditions are implied in this if-block
				 */
				{
					// Contract left
					State generatedState = new State(previousState);
					EntityAnnotation entity = generatedState.getEntity(entityID);
					entity.setBeginTokenIndex(from + 1);
					generatedStates.add(generatedState);
				}
				{
					// Contract right
					State generatedState = new State(previousState);
					EntityAnnotation entity = generatedState.getEntity(entityID);
					entity.setEndTokenIndex(to - 1);
					generatedStates.add(generatedState);
				}
			}
		}
		// add an unchanged state
		State generatedState = new State(previousState);
		generatedStates.add(generatedState);
		return generatedStates;
	}

	private List<State> generateStatesForNeighbors(State previousState) {
		List<State> generatedStates = new ArrayList<State>();
		Set<VariableID> previousStatesEntityIDs = previousState.getNonFixedEntityIDs();
		for (VariableID entityID1 : previousStatesEntityIDs) {
			EntityAnnotation previousStatesEntity1 = previousState.getEntity(entityID1);
			for (VariableID entityID2 : previousStatesEntityIDs) {
				EntityAnnotation previousStatesEntity2 = previousState.getEntity(entityID2);
				if (previousStatesEntity1.getType() == previousStatesEntity2.getType() && (previousStatesEntity1
						.getEndTokenIndex() == previousStatesEntity2.getBeginTokenIndex()
						|| previousStatesEntity2.getEndTokenIndex() == previousStatesEntity1.getBeginTokenIndex())) {
					State generatedState = new State(previousState);
					generatedState.removeEntity(entityID1);
					generatedState.removeEntity(entityID2);
					EntityAnnotation mergedEntity = new EntityAnnotation(generatedState, previousStatesEntity1);
					mergedEntity.setBeginTokenIndex(Math.min(previousStatesEntity1.getBeginTokenIndex(),
							previousStatesEntity2.getBeginTokenIndex()));
					mergedEntity.setEndTokenIndex(Math.min(previousStatesEntity1.getEndTokenIndex(),
							previousStatesEntity2.getEndTokenIndex()));
					generatedStates.add(generatedState);
				}
			}
		}
		return generatedStates;
	}

}
