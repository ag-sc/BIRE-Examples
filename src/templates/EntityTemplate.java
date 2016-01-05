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

public class EntityTemplate extends AbstractTemplate<State>implements Serializable {

	private static Logger log = LogManager.getFormatterLogger(EntityTemplate.class.getName());

	@Override
	public void computeFactor(State state, AbstractFactor abstractFactor) {
		// TODO features on unannotated tokens (thus, type/name = "null") might
		// be useful
		if (abstractFactor instanceof SingleVariableFactor) {

			SingleVariableFactor factor = (SingleVariableFactor) abstractFactor;
			EntityAnnotation entity = state.getEntity(factor.entityID);
			log.debug("%s: Add features to entity %s (\"%s\"):", this.getClass().getSimpleName(), entity.getID(),
					entity.getText());
			Vector featureVector = new Vector();

			String entityTypePrefix = "ENTITY_TYPE=" + entity.getType().getName() + "_";

			// featureVector.set(entityTypePrefix + "#ARGUMENTS>0",
			// entity.getReadOnlyArguments().size() > 0 ? 1.0 : 0.0);
			// featureVector.set(entityTypePrefix + "#ARGUMENTS>1",
			// entity.getReadOnlyArguments().size() > 1 ? 1.0 : 0.0);
			// featureVector.set(entityTypePrefix + "#ARGUMENTS>2",
			// entity.getReadOnlyArguments().size() > 2 ? 1.0 : 0.0);
			//
			// featureVector.set(entityTypePrefix + "#ARGUMENTS<=1",
			// entity.getReadOnlyArguments().size() <= 1 ? 1.0 : 0.0);
			// featureVector.set(entityTypePrefix + "#ARGUMENTS<=2",
			// entity.getReadOnlyArguments().size() <= 2 ? 1.0 : 0.0);

			featureVector.set(entityTypePrefix + "#Tokens<2", entity.getTokens().size() < 2 ? 1.0 : 0.0);
			featureVector.set(entityTypePrefix + "#Tokens<3", entity.getTokens().size() < 3 ? 1.0 : 0.0);
			featureVector.set(entityTypePrefix + "#Tokens<4", entity.getTokens().size() < 4 ? 1.0 : 0.0);

			featureVector.set(entityTypePrefix + "#Tokens>=1", entity.getTokens().size() >= 1 ? 1.0 : 0.0);
			featureVector.set(entityTypePrefix + "#Tokens>=2", entity.getTokens().size() >= 2 ? 1.0 : 0.0);
			featureVector.set(entityTypePrefix + "#Tokens>=3", entity.getTokens().size() >= 3 ? 1.0 : 0.0);
			featureVector.set(entityTypePrefix + "#Tokens>=4", entity.getTokens().size() >= 4 ? 1.0 : 0.0);

			log.debug("%s: Features for entity %s (\"%s\"): %s", this.getClass().getSimpleName(), entity.getID(),
					entity.getText(), featureVector);

			factor.setFeatures(featureVector);
		} else {
			log.warn("Provided factor with ID %s not of type SingleEntityFactor.", abstractFactor.getID());
		}
	}

	@Override
	protected Set<AbstractFactor> generateFactors(State state) {
		Set<AbstractFactor> factors = new HashSet<>();
		for (VariableID entityID : state.getEntityIDs()) {
			factors.add(new SingleVariableFactor(this, entityID));
		}
		return factors;
	}

}
