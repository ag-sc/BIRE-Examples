package sampler;

import corpus.Document;
import sampling.Initializer;
import variables.State;

public class DefaultInitializer<DocumentT extends Document<State>> implements Initializer<DocumentT, State> {

	private boolean usePriorKnowledge;

	public DefaultInitializer() {
		this.usePriorKnowledge = false;
	}

	public DefaultInitializer(boolean usePriorKnowledge) {
		this.usePriorKnowledge = usePriorKnowledge;
	}

	@Override
	public State getInitialState(DocumentT document) {
		if (usePriorKnowledge) {
			State priorKnowledge = document.getPriorKnowledge();
			return new State(priorKnowledge);
		} else {
			return new State(document);
		}
	}
}
