package templates;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.Token;
import factors.AbstractFactor;
import factors.TokenFactor;
import learning.Vector;
import variables.State;

public class TokenTemplate extends AbstractTemplate<State>implements Serializable {

	private static Logger log = LogManager.getFormatterLogger(TokenTemplate.class.getName());

	@Override
	public void computeFactor(State state, AbstractFactor abstractFactor) {
		if (abstractFactor instanceof TokenFactor) {

			TokenFactor factor = (TokenFactor) abstractFactor;
			Token token = state.getDocument().getTokens().get(factor.tokenIndex);
			double hasAnnotation = state.tokenHasAnnotation(factor.tokenIndex) ? 1.0 : 0.0;

			Vector featureVector = new Vector();

			String tokenPrefix = "TOKEN=" + token.getText() + "_";
			featureVector.set(tokenPrefix + "HAS_ANNOTATION", hasAnnotation);

			int[] suffixLengths = { 2, 3 };
			for (int i : suffixLengths) {
				featureVector.set(
						"TOKEN_WITH_SUFFIX_" + i + "=" + Features.suffix(token.getText(), i) + "_HAS_ANNOTATION",
						hasAnnotation);
			}

			int[] prefixLengths = { 2, 3 };
			for (int i : prefixLengths) {
				featureVector.set(
						"TOKEN_WITH_PREFIX_" + i + "=" + Features.prefix(token.getText(), i) + "_HAS_ANNOTATION",
						hasAnnotation);
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
			factors.add(new TokenFactor(this, i));
		}
		return factors;
	}

}
