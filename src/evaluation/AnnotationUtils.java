package evaluation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;

import corpus.FileUtils;
import corpus.SubDocument;
import corpus.Token;
import corpus.parser.bionlp.julie.Tokenization;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.State;

public class AnnotationUtils {

	private static Logger log = LogManager.getFormatterLogger(AnnotationUtils.class.getName());

	/**
	 * IN PROGRESS
	 * 
	 * @param state
	 * @param goldState
	 * @return
	 */
	public static double strictEquality(State state, State goldState) {
		Collection<EntityAnnotation> entities = state.getEntities();
		Collection<EntityAnnotation> goldEntities = goldState.getEntities();
		double tpGold = 0;
		double tpPredicted = 0;
		double fp = 0;
		double fn = 0;

		for (EntityAnnotation goldEntity : goldEntities) {
			boolean match = false;
			for (EntityAnnotation entity : entities) {
				match = matchEntities(entity, goldEntity);
				if (match)
					break;
			}
			if (match)
				tpGold++;
			else
				fn++;
		}

		for (EntityAnnotation entity : entities) {
			boolean match = false;
			for (EntityAnnotation goldEntity : goldEntities) {
				match = matchEntities(entity, goldEntity);
				if (match)
					break;
			}
			if (match)
				tpPredicted++;
			else
				fp++;
		}
		/*
		 * Count true positives separately and use the minimum. This is needed
		 * in the case, where we have multiple identical entities in one state
		 * that all match a single entity in the other. All of these identical
		 * entities would increase the true positives without, affecting false
		 * positives/negatives. An alternative would be to eliminate duplicates
		 * prior to measuring.
		 */

		double tp = Math.min(tpGold, tpPredicted);
		double precision = tp / (tp + fp);
		double recall = tp / (tp + fn);
		double f1 = 2 * (precision * recall) / (precision + recall);
		log.debug("TP: %s, FP: %s, FN: %s, P: %s, R: %s, F1: %s", tp, fp, fn, precision, recall, f1);
		return f1;
	}

	/**
	 * True, if these two entities match, false otherwise, given the strict
	 * equality defined at:
	 * http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/evaluation.shtml
	 * 
	 * 
	 * @param e1
	 * @param e2
	 * @return
	 */
	public static boolean matchEntities(EntityAnnotation e1, EntityAnnotation e2) {
		if (!Objects.equals(e1.getType().getName(), e2.getType().getName()))
			return false;
		if (e1.getBeginTokenIndex() != e2.getBeginTokenIndex() || e1.getEndTokenIndex() != e2.getEndTokenIndex())
			return false;
		if (!matchArguments(e1, e2))
			return false;
		return true;

	}

	/**
	 * True, if the arguments of both given entities match each other, false
	 * otherwise. More detail at:
	 * http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/evaluation.shtml
	 * 
	 * @param e1
	 * @param e2
	 * @return
	 */
	public static boolean matchArguments(EntityAnnotation e1, EntityAnnotation e2) {
		Multimap<ArgumentRole, VariableID> arguments1 = e1.getReadOnlyArguments();
		Multimap<ArgumentRole, VariableID> arguments2 = e2.getReadOnlyArguments();
		// this is a fast-reject test
		if (arguments1.size() != arguments2.size())
			return false;
		for (Entry<ArgumentRole, VariableID> argument1 : arguments1.entries()) {
			if (!containsArgument(e2, arguments2, e1, argument1))
				return false;
		}
		for (Entry<ArgumentRole, VariableID> argument2 : arguments2.entries()) {
			if (!containsArgument(e1, arguments1, e2, argument2))
				return false;
		}
		return true;
	}

	/**
	 * True, if the given Map of arguments <i>arguments1</i> of entity <i>e1</i>
	 * contains an argument that matches the argument <i>argument2</i> of entity
	 * <i>e2</i>, false otherwise. More detail at:
	 * http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/evaluation.shtml
	 * 
	 * @param e1
	 * @param arguments1
	 * @param e2
	 * @param argument2
	 * @return
	 */
	public static boolean containsArgument(EntityAnnotation e1, Multimap<ArgumentRole, VariableID> arguments1,
			EntityAnnotation e2, Entry<ArgumentRole, VariableID> argument2) {
		Collection<VariableID> possibleMatches = arguments1.get(argument2.getKey());
		for (VariableID entityID : possibleMatches) {
			if (matchEntities(e1.getEntity(entityID), e2.getEntity(argument2.getValue())))
				return true;
		}
		return false;
	}

}
