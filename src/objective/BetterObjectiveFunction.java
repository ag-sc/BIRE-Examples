package objective;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;

import learning.ObjectiveFunction;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.State;

@Deprecated
public class BetterObjectiveFunction extends ObjectiveFunction<State, State>implements Serializable {

	private static Logger log = LogManager.getFormatterLogger();

	/**
	 * WORK IN PROGRESS
	 */
	public BetterObjectiveFunction() {
	}

	@Override
	public double computeScore(State state, State goldState) {
		// TODO Discuss objective function implementation
		Collection<EntityAnnotation> entities = state.getEntities();
		Collection<EntityAnnotation> goldEntities = goldState.getEntities();
		double f1Score;
		if (goldEntities.size() == 0 && entities.size() == 0) {
			f1Score = 1;
		} else {
			double precision = 0.0;
			for (EntityAnnotation entity : entities) {
				double max = 0.0;
				for (EntityAnnotation goldEntity : goldEntities) {
					double typeScore = typeMatches(entity, goldEntity) ? 1.0 : 0.0;
					double overlapScore = overlapScore(entity, goldEntity);
					double argumentScore = argumentScore(entity, goldEntity);
					double entityScore = overlapScore * argumentScore * typeScore;
					if (max < entityScore) {
						max = entityScore;
					}
				}
				precision += max;
			}

			double recall = 0.0;
			for (EntityAnnotation goldEntity : goldEntities) {
				double max = 0.0;
				for (EntityAnnotation entity : entities) {
					double typeScore = typeMatches(goldEntity, entity) ? 1.0 : 0.0;
					double overlapScore = overlapScore(goldEntity, entity);
					double argumentScore = argumentScore(goldEntity, entity);

					double entityScore = overlapScore * argumentScore * typeScore;
					if (max < entityScore) {
						max = entityScore;
					}
				}
				recall += max;
			}
			// TODO score = 0 only because precision/recall = 0
			if ((precision == 0 && recall == 0) || entities.size() == 0 || goldEntities.size() == 0) {
				f1Score = 0;
			} else {
				precision /= entities.size();
				recall /= goldEntities.size();

				f1Score = 2 * (precision * recall) / (precision + recall);
			}
		}
		return f1Score;
	}

	private double argumentScore(EntityAnnotation entity1, EntityAnnotation entity2) {
		Multimap<ArgumentRole, VariableID> arguments1 = entity1.getReadOnlyArguments();
		Multimap<ArgumentRole, VariableID> arguments2 = entity2.getReadOnlyArguments();

		if (arguments1.keySet().size() == 0)
			return 1;

		int matchingRoles = 0;

		// count arguments of entity1 that are also in entity2
		for (Entry<ArgumentRole, VariableID> argument1 : arguments1.entries()) {
			ArgumentRole argRole1 = argument1.getKey();
			EntityAnnotation argEntity1 = entity1.getEntity(argument1.getValue());
			/*
			 * Since there are possibly several arguments with the same role,
			 * check if there is at least one that matches (overlaps)
			 * argEntity1.
			 */
			Collection<VariableID> argsForRole2 = arguments2.get(argRole1);
			for (VariableID argForRoleEntityID2 : argsForRole2) {
				EntityAnnotation argEntity2 = entity2.getEntity(argForRoleEntityID2);
				if (overlapScore(argEntity1, argEntity2) > 0) {
					matchingRoles++;
					// only count one match per argument
					break;
				}
			}
		}
		return matchingRoles / arguments1.size();
	}

	public static double overlapScore(EntityAnnotation entity, EntityAnnotation goldEntity) {
		int a = entity.getBeginTokenIndex();
		int b = entity.getEndTokenIndex();
		int x = goldEntity.getBeginTokenIndex();
		int y = goldEntity.getEndTokenIndex();
		double overlap = (double) overlap(entity, goldEntity);
		double overlapScore = overlap / (b - a);
		return overlapScore;
	}

	public static int overlap(EntityAnnotation entity1, EntityAnnotation entity2) {
		int a = entity1.getBeginTokenIndex();
		int b = entity1.getEndTokenIndex();
		int x = entity2.getBeginTokenIndex();
		int y = entity2.getEndTokenIndex();
		int overlap = Math.max(0, Math.min(b, y) - Math.max(a, x));
		return overlap;
	}

	public boolean typeMatches(EntityAnnotation entity, EntityAnnotation goldEntity) {
		return entity.getType().equals(goldEntity.getType());
	}

}
