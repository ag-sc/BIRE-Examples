package templates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import factors.AbstractFactor;
import factors.TokenFactor;
import factors.impl.UnorderedVariablesFactor;
import learning.Vector;
import variables.EntityAnnotation;
import variables.State;

public class MetaTemplate extends AbstractTemplate<State>implements Serializable {

	private static Logger log = LogManager.getFormatterLogger(MetaTemplate.class.getName());

	@Override
	public void computeFactor(State state, AbstractFactor abstractFactor) {
		if (abstractFactor instanceof UnorderedVariablesFactor) {
			UnorderedVariablesFactor factor = (UnorderedVariablesFactor) abstractFactor;
			Vector featureVector = new Vector();

			double ratio = ((double) state.getEntities().size()) / state.getDocument().getTokens().size();
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO<0.01", ratio < 0.01 ? 1.0 : 0.0);
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO<0.03", ratio < 0.03 ? 1.0 : 0.0);
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO<0.05", ratio < 0.05 ? 1.0 : 0.0);
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO<0.1", ratio < 0.1 ? 1.0 : 0.0);
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO<0.15", ratio < 0.15 ? 1.0 : 0.0);
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO<0.2", ratio < 0.2 ? 1.0 : 0.0);

			featureVector.set("ANNOTATION_TO_TOKEN_RATIO>=0.01", ratio >= 0.01 ? 1.0 : 0.0);
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO>=0.03", ratio >= 0.03 ? 1.0 : 0.0);
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO>=0.05", ratio >= 0.05 ? 1.0 : 0.0);
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO>=0.1", ratio >= 0.1 ? 1.0 : 0.0);
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO>=0.15", ratio >= 0.15 ? 1.0 : 0.0);
			featureVector.set("ANNOTATION_TO_TOKEN_RATIO>=0.2", ratio >= 0.2 ? 1.0 : 0.0);

			List<EntityAnnotation> entities = new ArrayList<>(state.getEntities());
			for (int i = 0; i < entities.size(); i++) {
				EntityAnnotation t1 = entities.get(i);
				for (int j = i + 1; j < entities.size(); j++) {
					EntityAnnotation t2 = entities.get(j);
					String text1 = null;
					String text2 = null;
					if (t1.getType().getName().compareTo(t2.getType().getName()) < 0) {
						text1 = t1.getType().getName();
						text2 = t2.getType().getName();
					} else {
						text1 = t2.getType().getName();
						text2 = t1.getType().getName();
					}
					featureVector.set("TYPE=" + text1 + "_AND_TYPE=" + text2 + "_CO-OCCUR", 1.0);
				}
			}

			factor.setFeatures(featureVector);
		} else {
			log.warn("Provided factor with ID %s not of type %s.", abstractFactor.getID(),
					TokenFactor.class.getSimpleName());
		}
	}

	@Override
	protected Set<AbstractFactor> generateFactors(State state) {
		Set<AbstractFactor> factors = new HashSet<>();
		for (int i = 0; i < state.getDocument().getTokens().size(); i++) {
			factors.add(new UnorderedVariablesFactor(this, state.getEntityIDs()));
		}
		return factors;
	}

}
