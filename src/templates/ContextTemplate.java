package templates;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;

import changes.StateChange;
import corpus.Token;
import factors.AbstractFactor;
import factors.impl.SingleVariableFactor;
import learning.Vector;
import utility.VariableID;
import variables.EntityAnnotation;
import variables.State;

public class ContextTemplate extends AbstractTemplate<State>implements Serializable {

	private static Logger log = LogManager.getFormatterLogger(ContextTemplate.class.getName());

	public final Set<StateChange> relevantChanges = Sets.newHashSet(StateChange.ADD_ANNOTATION,
			StateChange.CHANGE_BOUNDARIES, StateChange.CHANGE_TYPE, StateChange.REMOVE_ANNOTATION);

	@Override
	public void computeFactor(State state, AbstractFactor abstractFactor) {
		if (abstractFactor instanceof SingleVariableFactor) {

			SingleVariableFactor factor = (SingleVariableFactor) abstractFactor;
			EntityAnnotation entity = state.getEntity(factor.entityID);
			log.debug("%s: Add features to entity %s (\"%s\"):", this.getClass().getSimpleName(), entity.getID(),
					entity.getText());

			Vector featureVector = new Vector();

			String entityTypePrefix = "ENTITY_TYPE=" + entity.getType().getName() + "_";

			int[] tokenOffsets = { -2, -1, 1, 2 };
			for (int i : tokenOffsets) {
				Token tokenAt = Features.getTokenRelativeToEntity(state, entity, i);
				if (tokenAt != null) {
					String at = i > 0 ? "+" + String.valueOf(i) : String.valueOf(i);
					featureVector.set(entityTypePrefix + "TOKEN@" + at + "_EQUALS=" + tokenAt.getText(), 1.0);
				}
			}

			log.debug("%s: Features for entity %s (\"%s\"): %s", this.getClass().getSimpleName(), entity.getID(),
					entity.getText(), featureVector);
			factor.setFeatures(featureVector);
		} else {
			log.warn("Provided factor with ID %s not of type SingleEntityFactor.", abstractFactor.getID());
		}
	}


	@Override
	protected Set<AbstractFactor> generateFactors(State state) {
		Set<AbstractFactor> variableSets = new HashSet<>();
		for (VariableID entityID : state.getEntityIDs()) {
			variableSets.add(new SingleVariableFactor(this, entityID));
		}
		return variableSets;
	}
}
