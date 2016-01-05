package sampler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashMultimap;

import evaluation.AnnotationUtils;
import sampling.Explorer;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.State;

public class ExhaustiveBoundaryExplorer implements Explorer<State> {

	private static Logger log = LogManager.getFormatterLogger(ExhaustiveBoundaryExplorer.class.getName());

	private boolean mergeNeighbors;
	private boolean noOverlaps;

	public ExhaustiveBoundaryExplorer() {
		this.mergeNeighbors = false;
		this.noOverlaps = false;
	}

	public ExhaustiveBoundaryExplorer(boolean mergeNeighbors, boolean noOverlaps) {
		this.mergeNeighbors = mergeNeighbors;
		this.noOverlaps = noOverlaps;
	}

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
		Set<VariableID> entities = previousState.getEditableEntityIDs();
		for (VariableID entityID : entities) {
			EntityAnnotation previousStatesEntity = previousState.getEntity(entityID);
			int from = previousStatesEntity.getBeginTokenIndex();
			int to = previousStatesEntity.getEndTokenIndex();
			if (0 < from) {
				if (!noOverlaps || !previousState.tokenHasAnnotation(from - 1)) {
					// Expand left
					State generatedState = new State(previousState);
					EntityAnnotation entity = generatedState.getEntity(entityID);
					entity.setBeginTokenIndex(from - 1);
					generatedStates.add(generatedState);
				}
			}
			if (to < previousState.getDocument().getTokens().size()) {
				if (!noOverlaps || !previousState.tokenHasAnnotation(to)) {
					// Expand right
					State generatedState = new State(previousState);
					EntityAnnotation entity = generatedState.getEntity(entityID);
					entity.setEndTokenIndex(to + 1);
					generatedStates.add(generatedState);
				}
			}
			if (to - from > 1) {
				/**
				 * Here we just assume that to >= from. That is why we do not
				 * check if to > 0 or from < "max"-1. Given a consistent state,
				 * these conditions are implied in this if-block
				 */
				{
					if (!noOverlaps || !previousState.tokenHasAnnotation(from + 1)) {
						// Contract left
						State generatedState = new State(previousState);
						EntityAnnotation entity = generatedState.getEntity(entityID);
						entity.setBeginTokenIndex(from + 1);
						generatedStates.add(generatedState);
					}
				}
				{
					if (!noOverlaps || !previousState.tokenHasAnnotation(to - 1)) {
						// Contract right
						State generatedState = new State(previousState);
						EntityAnnotation entity = generatedState.getEntity(entityID);
						entity.setEndTokenIndex(to - 1);
						generatedStates.add(generatedState);
					}
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
		Set<VariableID> previousStatesEntityIDs = previousState.getEditableEntityIDs();
		// avoid duplicate merges by tracking all merged variable pairs
		Set<MergedVariablePair> mergedVariablePairs = new HashSet<>();
		for (VariableID entityID1 : previousStatesEntityIDs) {
			EntityAnnotation previousStatesEntity1 = previousState.getEntity(entityID1);
			for (VariableID entityID2 : previousStatesEntityIDs) {
				EntityAnnotation previousStatesEntity2 = previousState.getEntity(entityID2);
				if (!mergedVariablePairs.contains(new MergedVariablePair(entityID1, entityID2))
						&& areMatchingNeighbors(previousStatesEntity1, previousStatesEntity2)) {
					State generatedState = new State(previousState);
					generatedState.removeEntity(entityID1);
					generatedState.removeEntity(entityID2);
					int beginIndex = Math.min(previousStatesEntity1.getBeginTokenIndex(),
							previousStatesEntity2.getBeginTokenIndex());
					int endIndex = Math.max(previousStatesEntity1.getEndTokenIndex(),
							previousStatesEntity2.getEndTokenIndex());
					HashMultimap<ArgumentRole, VariableID> arguments = HashMultimap
							.create(previousStatesEntity1.getReadOnlyArguments());

					EntityAnnotation mergedEntity = new EntityAnnotation(generatedState,
							previousStatesEntity1.getType(), arguments, beginIndex, endIndex);
					generatedState.addEntity(mergedEntity);
					generatedStates.add(generatedState);
					mergedVariablePairs.add(new MergedVariablePair(entityID1, entityID2));
				}
			}
		}
		return generatedStates;
	}

	/**
	 * Checks if the provided entities are direct neighbors (beginning of the
	 * one is end of the other), and have same types and arguments.
	 * 
	 * @param entity1
	 * @param entity2
	 * @return
	 */
	private boolean areMatchingNeighbors(EntityAnnotation entity1, EntityAnnotation entity2) {

		if (entity1.getID() == entity2.getID()) {
			return false;
		}
		if (!Objects.equals(entity1.getType(), entity2.getType())) {
			return false;
		}
		if (entity1.getEndTokenIndex() != entity2.getBeginTokenIndex()
				&& entity2.getEndTokenIndex() != entity1.getBeginTokenIndex()) {
			return false;
		}
		if (!AnnotationUtils.matchArguments(entity1, entity2)) {
			return false;
		}
		return true;
	}

	class MergedVariablePair {
		Set<VariableID> entities = new HashSet<>();

		public MergedVariablePair(VariableID enitity1, VariableID enitity2) {
			super();
			entities.add(enitity1);
			entities.add(enitity2);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((entities == null) ? 0 : entities.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MergedVariablePair other = (MergedVariablePair) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (entities == null) {
				if (other.entities != null)
					return false;
			} else if (!entities.equals(other.entities))
				return false;
			return true;
		}

		private ExhaustiveBoundaryExplorer getOuterType() {
			return ExhaustiveBoundaryExplorer.this;
		}

	}

}
