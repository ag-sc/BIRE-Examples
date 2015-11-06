package templates;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.LabeledDocument;
import factors.AbstractFactor;
import factors.impl.UnorderedVariablesFactor;
import learning.ObjectiveFunction;
import learning.Vector;
import variables.State;

public class ObjectiveFunctionTemplate extends AbstractTemplate<State>implements Serializable {

	private static Logger log = LogManager.getFormatterLogger(ObjectiveFunctionTemplate.class.getName());
	private static final String GOLD = "GOLD";

	private ObjectiveFunction<State, State> objective;

	public ObjectiveFunctionTemplate(ObjectiveFunction<State, State> objective) {
		this.objective = objective;
	}

	@Override
	public void computeFactor(State state, AbstractFactor factor) {
		Vector featureVector = new Vector();
		if (state.getDocument() instanceof LabeledDocument) {
			State goldState = ((LabeledDocument<State, State>) state.getDocument()).getGoldResult();
			double score = objective.score(state, goldState);
			featureVector.set(GOLD, score);

			factor.setFeatures(featureVector);
		} else {
			log.warn(
					"Template %s: Given state does not have an AnnotatedDocument attached. Cheating template not applicable.",
					this.getClass().getSimpleName());
		}
	}

	@Override
	protected Set<AbstractFactor> generateFactors(State state) {
		Set<AbstractFactor> factors = new HashSet<>();
		factors.add(new UnorderedVariablesFactor(this, state.getEntityIDs()));
		return factors;
	}
}
