package sampler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;

import changes.StateChange;
import corpus.AnnotationConfig;
import corpus.EntityTypeDefinition;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.EntityAnnotation;
import variables.State;

@Deprecated
public class SamplingHelper {
	private static Logger log = LogManager.getFormatterLogger(SamplingHelper.class.getName());

	public enum BoundaryChange {
		EXPAND_LEFT, EXPAND_RIGHT, CONTRACT_LEFT, CONTRACT_RIGHT, DO_NOTHING;
	}

	public static State drawRandomlyFrom(List<State> nextStates) {
		Map<State, Double> stateMap = new HashMap<State, Double>();
		for (State state : nextStates) {
			stateMap.put(state, state.getModelScore());
		}
		return drawRandomlyFrom(stateMap);
	}

	public static State drawRandomlyFrom(Map<State, Double> nextStates) {
		List<Entry<State, Double>> listOfStates = new ArrayList<Map.Entry<State, Double>>(nextStates.entrySet());

		// compute total sum of scores
		double totalSum = 0;
		for (Entry<State, Double> e : listOfStates) {
			totalSum += e.getValue();
		}

		double index = Math.random() * totalSum;
		double sum = 0;
		int i = 0;
		while (sum < index) {
			sum += listOfStates.get(i++).getValue();
		}
		return listOfStates.get(Math.max(0, i - 1)).getKey();
	}

	public static boolean accept(State nextState, State state, double acceptanceFactor) {
		if (nextState.getModelScore() > state.getModelScore()) {
			// if new state is better, always accept
			return true;
		} else {
			/*-
			 * if new state is worse, accept with probability p(accept).
			 * p(accept) = exp(-(scoreNew - scoreOld)/acceptanceFactor)
			 */
			double pNext = Math.exp(-(nextState.getModelScore() - state.getModelScore() / acceptanceFactor));
			if (Math.random() < pNext)
				return true;
			else
				return false;
		}
	}

	// public static void addRandomAnnotation(Token sampledToken, State state) {
	//
	// EntityType sampledType = sampleEntityType(state);
	// EntityAnnotation tokenAnnotation = new
	// EntityAnnotation(state, sampledType,
	// sampledToken.getIndex(), sampledToken.getIndex() + 1);
	// state.addMutableEntity(tokenAnnotation);
	// }

	/**
	 * Chooses a unassigned role for the given EntityType and an existing
	 * annotation from the given state and add the pair as a new argument to the
	 * given entity.
	 * 
	 * @param entity
	 * @param state
	 * @param corpusConfig
	 */
	public static void addRandomArgument(EntityAnnotation entity, State state, AnnotationConfig corpusConfig) {
		// get all possible argument targets
		List<EntityAnnotation> entities = new ArrayList<>(state.getEntities());
		entities.remove(entity);
		if (!entities.isEmpty()) {
			List<ArgumentRole> unassignedRoles = new ArrayList<>();
			EntityTypeDefinition entityTypeDefinition = corpusConfig.getEntityTypeDefinition(entity.getType());
			unassignedRoles.addAll(entityTypeDefinition.getCoreArguments().keySet());
			unassignedRoles.addAll(entityTypeDefinition.getOptionalArguments().keySet());
			// remove already assigned roles
			unassignedRoles.removeAll(entity.getReadOnlyArguments().keySet());

			if (!unassignedRoles.isEmpty()) {
				ArgumentRole sampledRole = getRandomElement(unassignedRoles);

				EntityAnnotation sampledEntity = getRandomElement(entities);
				entity.addArgument(sampledRole, sampledEntity.getID());
				log.debug("\t%s + %s:%s", entity.getID(), sampledRole, sampledEntity.getID());
			} else {
				log.debug("\t%s (%s): No unassigned arguments left", entity.getID(), entity.getType().getName());
			}
		} else {
			log.debug("\t%s (%s): No entities for argument existing", entity.getID(), entity.getType().getName());
		}
	}

	/**
	 * Chooses an existing argument (role and entity) randomly and removes it
	 * from the Map of arguments for this entity. Note that other annotation may
	 * still reference this argument.
	 * 
	 * @param tokenAnnotation
	 */
	public static void removeRandomArgument(EntityAnnotation tokenAnnotation) {
		if (!tokenAnnotation.getReadOnlyArguments().isEmpty()) {
			Multimap<ArgumentRole, VariableID> arguments = tokenAnnotation.getReadOnlyArguments();
			List<Entry<ArgumentRole, VariableID>> roles = new ArrayList<>(arguments.entries());
			if (!roles.isEmpty()) {
				Entry<ArgumentRole, VariableID> sampledArgument = getRandomElement(roles);
				tokenAnnotation.removeArgument(sampledArgument.getKey(), sampledArgument.getValue());
				// log.warn("\t%s: (%s): This type is not supposed to have
				// arguments (yet, it seems to have some. If this message shows,
				// something is not working consistently)",
				// tokenAnnotation.getID(),
				// tokenAnnotation.getType().getName());
			}
		} else {
			log.debug("\t%s (%s): No arguments assigned, yet", tokenAnnotation.getID(),
					tokenAnnotation.getType().getName());
		}
	}
	//
	// public static boolean changeBoundaries(EntityAnnotation
	// tokenAnnotation, State state) {
	// // the boundaries of annotations are on token level!
	// switch (sampleBoundaryChange(tokenAnnotation)) {
	// case EXPAND_LEFT:
	// log.debug("\texpand left");
	// tokenAnnotation.setBeginTokenIndex(tokenAnnotation.getBeginTokenIndex() -
	// 1);
	// return true;
	// case CONTRACT_LEFT:
	// log.debug("\tcontract left");
	// tokenAnnotation.setBeginTokenIndex(tokenAnnotation.getBeginTokenIndex() +
	// 1);
	// return true;
	// case EXPAND_RIGHT:
	// log.debug("\texpand right");
	// tokenAnnotation.setEndTokenIndex(tokenAnnotation.getEndTokenIndex() + 1);
	// return true;
	// case CONTRACT_RIGHT:
	// log.debug("\tcontract right");
	// tokenAnnotation.setEndTokenIndex(tokenAnnotation.getEndTokenIndex() - 1);
	// return true;
	// case DO_NOTHING:
	// log.debug("Do not change entity boundary");
	// default:
	// }
	// return false;
	// }
	//
	// /**
	// * Samples an EntityType from the list of all possible Entity Types that
	// is
	// * given via the AnnotationConfig. This object is accessible through the
	// * Document/Corpus in which this State is situated.
	// *
	// * @param state
	// * @return
	// */
	// public static EntityType sampleEntityType(State state) {
	// List<EntityType> possibleEntityTypes = new ArrayList<EntityType>(
	// state.getDocument().getCorpus().getCorpusConfig().getEntityTypes());
	//
	// EntityType randomType = possibleEntityTypes.get((int) (Math.random() *
	// possibleEntityTypes.size()));
	// return randomType;
	// }

	/**
	 * Chooses one of the state changes as defined in {@link StateChange}
	 * according to a uniform distribution.
	 * 
	 * @return
	 */
	public static StateChange sampleStateChange(StateChange... stateChanges) {
		if (stateChanges == null || stateChanges.length == 0) {
			stateChanges = StateChange.values();
		}
		int randomIndex = (int) (Math.random() * stateChanges.length);
		return stateChanges[randomIndex];
	}

	// public static BoundaryChange sampleBoundaryChange(EntityAnnotation
	// entity) {
	// List<BoundaryChange> possibleBoundaryChanges = new
	// ArrayList<BoundaryChange>();
	// // possibleBoundaryChanges.add(BoundaryChange.DO_NOTHING);
	// if (entity.getBeginTokenIndex() > 0)
	// possibleBoundaryChanges.add(BoundaryChange.EXPAND_LEFT);
	// if (entity.getEndTokenIndex() <
	// entity.getState().getDocument().getTokens().size())
	// possibleBoundaryChanges.add(BoundaryChange.EXPAND_RIGHT);
	// if (entity.getEndTokenIndex() - entity.getBeginTokenIndex() > 0) {
	// possibleBoundaryChanges.add(BoundaryChange.CONTRACT_LEFT);
	// possibleBoundaryChanges.add(BoundaryChange.CONTRACT_RIGHT);
	// }
	// return getRandomElement(possibleBoundaryChanges);
	// }

	/**
	 * Returns an element off the specified list that is selected according to a
	 * uniform distribution.
	 * 
	 * @param list
	 * @return
	 */
	public static <T> T getRandomElement(List<T> list) {
		if (list.isEmpty())
			return null;
		return list.get((int) (Math.random() * list.size()));
	}

	public static List<State> getBest(List<State> states, int n) {
		return states.subList(0, Math.min(n, states.size()));
	}

	public static List<State> getBest(List<State> states, int n, boolean sort) {
		if (sort)
			Collections.sort(states, State.modelScoreComparator);
		return getBest(states, n);
	}

	public static void changeRandomArgumentRole(EntityAnnotation entity, State state, AnnotationConfig corpusConfig) {
		List<ArgumentRole> possibleNewRoles = new ArrayList<>();
		EntityTypeDefinition type = corpusConfig.getEntityTypeDefinition(entity.getType());
		possibleNewRoles.addAll(type.getCoreArguments().keySet());
		possibleNewRoles.addAll(type.getOptionalArguments().keySet());

		List<Entry<ArgumentRole, VariableID>> arguments = new ArrayList<>(entity.getReadOnlyArguments().entries());
		if (!arguments.isEmpty()) {
			// select random existing argument by role
			Entry<ArgumentRole, VariableID> argumentToChange = getRandomElement(arguments);
			// remove current role from possible alternatives
			possibleNewRoles.remove(argumentToChange.getKey());
			// sample from alternatives
			ArgumentRole sampledNewRole = getRandomElement(possibleNewRoles);
			// remove already assigned roles
			// unassignedRoles.removeAll(entity.getArguments().keySet());

			if (!possibleNewRoles.isEmpty()) {
				entity.removeArgument(argumentToChange.getKey(), argumentToChange.getValue());
				entity.addArgument(sampledNewRole, argumentToChange.getValue());
				log.debug("\tChange role from %s to %s for argument entity %s", argumentToChange.getKey(),
						sampledNewRole, argumentToChange.getValue());
			} else {

				log.debug("\t%s (%s): No unassigned arguments left", entity.getID(), entity.getType().getName());
			}
		} else {
			log.debug("\tEntity has no arguments assigned, yet.");
		}
	}

	public static void changeRandomArgumentEntity(EntityAnnotation entity, State state, AnnotationConfig corpusConfig) {
		List<Entry<ArgumentRole, VariableID>> arguments = new ArrayList<>(entity.getReadOnlyArguments().entries());
		if (!arguments.isEmpty()) {
			// select random existing argument by role
			Entry<ArgumentRole, VariableID> argumentToChange = getRandomElement(arguments);

			// list alternative argument entities
			List<VariableID> entityIDs = new ArrayList<VariableID>(state.getEntityIDs());

			// remove parent entity from list of possible arguments
			entityIDs.remove(entity.getID());
			// remove previous entity from list of possible arguments
			entityIDs.remove(argumentToChange.getValue());

			if (!entityIDs.isEmpty()) {
				VariableID newArgumentID = getRandomElement(entityIDs);
				// replaces the previous entity with the new one
				entity.addArgument(argumentToChange.getKey(), newArgumentID);
				log.debug("\tChange entity of argument %s from entity %s to %s", argumentToChange.getKey(),
						argumentToChange.getValue(), newArgumentID);
			} else {

				log.debug("\t%s (%s): No entities for argument existing", entity.getID(), entity.getType().getName());
			}
		} else {
			log.debug("\tEntity has no arguments assigned, yet.");
		}
	}
}
