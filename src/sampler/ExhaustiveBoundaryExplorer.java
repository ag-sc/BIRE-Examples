package sampler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sampling.Explorer;
import utility.VariableID;
import variables.MutableEntityAnnotation;
import variables.State;

public class ExhaustiveBoundaryExplorer implements Explorer<State> {

	private static Logger log = LogManager.getFormatterLogger(ExhaustiveBoundaryExplorer.class.getName());

	public List<State> getNextStates(State previousState) {

		List<State> generatedStates = new ArrayList<State>();
		Set<VariableID> entities = previousState.getMutableEntityIDs();
		for (VariableID entityID : entities) {
			MutableEntityAnnotation previousStatesEntity = previousState.getMutableEntity(entityID);
			int from = previousStatesEntity.getBeginTokenIndex();
			int to = previousStatesEntity.getEndTokenIndex();
			if (0 < from) {
				// Expand left
				State generatedState = new State(previousState);
				MutableEntityAnnotation entity = generatedState.getMutableEntity(entityID);
				entity.setBeginTokenIndex(from - 1);
				generatedStates.add(generatedState);
			}
			if (to < previousStatesEntity.getState().getDocument().getTokens().size()) {
				// Expand right
				State generatedState = new State(previousState);
				MutableEntityAnnotation entity = generatedState.getMutableEntity(entityID);
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
					MutableEntityAnnotation entity = generatedState.getMutableEntity(entityID);
					entity.setBeginTokenIndex(from + 1);
					generatedStates.add(generatedState);
				}
				{
					// Contract right
					State generatedState = new State(previousState);
					MutableEntityAnnotation entity = generatedState.getMutableEntity(entityID);
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

}
