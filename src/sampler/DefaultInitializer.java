package sampler;

import corpus.Document;
import sampling.Initializer;
import variables.State;

public class DefaultInitializer implements Initializer<State, State> {

	private boolean usePriorKnowledge = false;

	public DefaultInitializer() {
	}

	public DefaultInitializer(boolean usePriorKnowledge) {
		this.usePriorKnowledge = usePriorKnowledge;
	}

	@Override
	public State getInitialState(Document<State> document) {
		if (usePriorKnowledge) {
			State priorKnowledge = document.getPriorKnowledge();
			return new State(priorKnowledge);
		} else {
			return new State(document);
		}
	}
}
