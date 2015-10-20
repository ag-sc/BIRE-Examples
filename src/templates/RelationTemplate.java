package templates;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import changes.StateChange;
import factors.AbstractFactor;
import factors.EntityAndArgumentFactor;
import learning.Vector;
import logging.Log;
import templates.AbstractTemplate;
import utility.VariableID;
import variables.AbstractEntityAnnotation;
import variables.ArgumentRole;
import variables.EntityType;
import variables.State;

public class RelationTemplate extends AbstractTemplate<State>implements Serializable {

	private static Logger log = LogManager.getFormatterLogger(RelationTemplate.class.getName());

	public final Set<StateChange> relevantChanges = Sets.newHashSet(StateChange.values());

	@Override
	public void computeFactor(State state, AbstractFactor abstractFactor) {
		// TODO features on unannotated tokens (thus, type/name = "null") might
		// be useful
		if (abstractFactor instanceof EntityAndArgumentFactor) {
			EntityAndArgumentFactor factor = (EntityAndArgumentFactor) abstractFactor;
			AbstractEntityAnnotation mainEntity = state.getEntity(factor.getMainEntityID());
			AbstractEntityAnnotation argEntity = state.getEntity(factor.getArgumentEntityID());
			ArgumentRole argRole = factor.getArgumentRole();

			EntityType argType = argEntity.getType();

			Log.d("%s: Add features to entity %s (\"%s\"):", this.getClass().getSimpleName(), mainEntity.getID(),
					mainEntity.getText());

			Vector featureVector = new Vector();

			EntityType entityType = mainEntity.getType();
			String entityAsText = mainEntity.getText();

			/*
			 * The next few features are always present for each individual
			 * token, thus, they always have a value of 1
			 */
			featureVector.set("ENTITY_TYPE=" + entityType.getName() + " & ARG_TYPE=" + argType.getName(), 1.0);
			featureVector.set("ENTITY_TYPE=" + entityType.getName() + " & ARG_ROLE=" + argRole, 1.0);
			featureVector.set("ENTITY_TYPE=" + entityType.getName() + " & ARG_TYPE=" + argType.getName()
					+ " & ARG_ROLE=" + argRole, 1.0);

			for (int i = 0; i < 4; i++) {
				double distanceFeatureValue = Math.abs(distance(mainEntity, argEntity)) > i ? 1 : 0;
				featureVector.set("DISTANCE_FROM_ENTITY=" + entityAsText + "_TO_ARGUMENT_ROLE=" + argRole + ">" + i,
						distanceFeatureValue);
				// featureVector.set(
				// "DISTANCE_FROM_ENTITY_TO_ARGUMENT_ROLE="
				// + argRole + ">" + i,
				// distanceFeatureValue);
			}

			featureVector.set("ENTITY=" + entityAsText + "_BEFORE_ARGUMENT_ROLE=" + argRole,
					isBefore(mainEntity, argEntity));
			featureVector.set("ENTITY_BEFORE_ARGUMENT_ROLE=" + argRole, isBefore(mainEntity, argEntity));

			featureVector.set("ENTITY=" + entityAsText + "_AFTER_ARGUMENT_ROLE=" + argRole,
					isAfter(mainEntity, argEntity));
			featureVector.set("ENTITY_AFTER_ARGUMENT_ROLE=" + argRole, isAfter(mainEntity, argEntity));

			Log.d("%s: Features for entity %s (\"%s\"): %s", this.getClass().getSimpleName(), mainEntity.getID(),
					mainEntity.getText(), featureVector);
			factor.setFeatures(featureVector);
		} else {
			log.warn("Provided factor with ID %s not of type EntityAndArgumentFactor.", abstractFactor.getID());
		}

	}

	private double isBefore(AbstractEntityAnnotation e1, AbstractEntityAnnotation e2) {
		return e1.getEndTokenIndex() <= e2.getBeginTokenIndex() ? 1.0 : 0;
	}

	private double isAfter(AbstractEntityAnnotation e1, AbstractEntityAnnotation e2) {
		return e2.getEndTokenIndex() <= e1.getBeginTokenIndex() ? 1.0 : 0;
	}

	private int distance(AbstractEntityAnnotation e1, AbstractEntityAnnotation e2) {
		// TODO test implementation of entity distance
		return Math.max(e2.getBeginTokenIndex() - e1.getEndTokenIndex() + 1,
				e1.getBeginTokenIndex() - e2.getEndTokenIndex() + 1);
	}

	@Override
	protected boolean isRelevantChange(StateChange value) {
		return relevantChanges.contains(value);
	}

	@Override
	protected Set<AbstractFactor> generateFactors(State state) {
		Set<AbstractFactor> factors = new HashSet<>();
		for (AbstractEntityAnnotation entity : state.getEntities()) {
			Multimap<ArgumentRole, VariableID> arguments = entity.getArguments();
			for (Entry<ArgumentRole, VariableID> a : arguments.entries()) {
				factors.add(new EntityAndArgumentFactor(this, entity.getID(), a.getKey(), a.getValue()));
			}
		}
		return factors;
	}
}
