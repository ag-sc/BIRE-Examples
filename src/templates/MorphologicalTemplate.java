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

public class MorphologicalTemplate extends AbstractTemplate<State>implements Serializable {

	private static Logger log = LogManager.getFormatterLogger(MorphologicalTemplate.class.getName());

	public final Set<StateChange> relevantChanges = Sets.newHashSet(StateChange.ADD_ANNOTATION,
			StateChange.CHANGE_BOUNDARIES, StateChange.CHANGE_TYPE, StateChange.REMOVE_ANNOTATION);

	@Override
	public void computeFactor(State state, AbstractFactor abstractFactor) {
		// TODO features on unannotated tokens (thus, type/name = "null") might
		// be useful
		if (abstractFactor instanceof SingleVariableFactor) {

			SingleVariableFactor factor = (SingleVariableFactor) abstractFactor;
			EntityAnnotation entity = state.getEntity(factor.entityID);
			log.debug("%s: Add features to entity %s (\"%s\"):", this.getClass().getSimpleName(), entity.getID(),
					entity.getText());
			log.debug("%s: Add features to entity %s (\"%s\"):", this.getClass().getSimpleName(), entity.getID(),
					entity.getText());
			Vector featureVector = new Vector();

			List<Token> tokens = entity.getTokens();
			Token first = tokens.get(0);
			Token last = tokens.get(tokens.size() - 1);
			String entityTypePrefix = "ENTITY_TYPE=" + entity.getType().getName() + "_";
			featureVector.set(entityTypePrefix + "ALL_TOKENS_INIT_CAP", Features.StartsWithCapital.all(tokens));
			featureVector.set(entityTypePrefix + "AT_LEAST_ONE_TOKEN_INIT_CAP", Features.StartsWithCapital.any(tokens));
			featureVector.set(entityTypePrefix + "FIRST_TOKEN_INIT_CAP", Features.StartsWithCapital.first(tokens));
			featureVector.set(entityTypePrefix + "LAST_TOKEN_INIT_CAP", Features.StartsWithCapital.last(tokens));

			featureVector.set(entityTypePrefix + "ALL_TOKENS_ALL_CAP", Features.AllCapital.all(tokens));
			featureVector.set(entityTypePrefix + "AT_LEAST_ONE_TOKEN_ALL_CAP", Features.AllCapital.any(tokens));
			featureVector.set(entityTypePrefix + "FIRST_TOKEN_ALL_CAP", Features.AllCapital.first(tokens));
			featureVector.set(entityTypePrefix + "LAST_TOKEN_ALL_CAP", Features.AllCapital.last(tokens));

			featureVector.set(entityTypePrefix + "ALL_TOKENS_CONTAIN_DIGIT", Features.ContainsDigit.all(tokens));
			featureVector.set(entityTypePrefix + "AT_LEAST_ONE_TOKEN_CONTAINS_DIGIT",
					Features.ContainsDigit.any(tokens));
			featureVector.set(entityTypePrefix + "FIRST_TOKEN_CONTAINS_DIGIT", Features.ContainsDigit.first(tokens));
			featureVector.set(entityTypePrefix + "LAST_TOKEN_CONTAINS_DIGIT", Features.ContainsDigit.last(tokens));

			featureVector.set(entityTypePrefix + "AT_LEAST_ONE_TOKEN_CONTAINS_HYPHEN",
					Features.ContainsHyphen.any(tokens));
			featureVector.set(entityTypePrefix + "AT_LEAST_ONE_TOKEN_CONTAINS_PUNCTUATION",
					Features.ContainsPunctuation.any(tokens));
			featureVector.set(entityTypePrefix + "AT_LEAST_ONE_TOKEN_CONTAINS_GREEK_SYMBOL",
					Features.ContainsGreek.any(tokens));

			/*
			 * The following features are always present for each individual
			 * token, thus, they always have a value of 1
			 */

			int[] suffixLengths = { 2, 3 };
			for (int i : suffixLengths) {
				if (last.getText().length() >= i)
					featureVector.set(
							entityTypePrefix + "LAST_TOKEN_SUFFIX_" + i + "=" + Features.suffix(last.getText(), i),
							1.0);
				if (first.getText().length() >= i)
					featureVector.set(
							entityTypePrefix + "FIRST_TOKEN_SUFFIX_" + i + "=" + Features.suffix(first.getText(), i),
							1.0);
			}

			int[] prefixLengths = { 2, 3 };
			for (int i : prefixLengths) {
				if (last.getText().length() >= i)
					featureVector.set(
							entityTypePrefix + "LAST_TOKEN_PREFIX_" + i + "=" + Features.prefix(last.getText(), i),
							1.0);
				if (first.getText().length() >= i)
					featureVector.set(
							entityTypePrefix + "FIRST_TOKEN_PREFIX_" + i + "=" + Features.prefix(first.getText(), i),
							1.0);
			}

			// for (int i = 0; i < tokens.size(); i++) {
			// featureVector.set(entityTypePrefix + "CONTAINS_TOKEN=" +
			// tokens.get(i).getText(), 1.0);
			// }

			for (int i = 0; i < tokens.size(); i++) {
				Token t1 = tokens.get(i);
				for (int j = i + 1; j < tokens.size(); j++) {
					Token t2 = tokens.get(j);
					String text1 = null;
					String text2 = null;
					if (t1.getText().compareTo(t2.getText()) < 0) {
						text1 = t1.getText();
						text2 = t2.getText();
					} else {
						text1 = t2.getText();
						text2 = t1.getText();
					}
					featureVector.set(entityTypePrefix + "TOKEN=" + text1 + "_AND_TOKEN=" + text2 + "_CO-OCCUR", 1.0);
				}
			}
			featureVector.set(entityTypePrefix + "FIRST_TOKEN_EQUALS=" + first.getText(), 1.0);
			featureVector.set(entityTypePrefix + "LAST_TOKEN_EQUALS=" + last.getText(), 1.0);

			log.debug("%s: Features for entity %s (\"%s\"): %s", this.getClass().getSimpleName(), entity.getID(),
					entity.getText(), featureVector);

			factor.setFeatures(featureVector);
		} else {
			log.warn("Provided factor with ID %s not of type SingleEntityFactor.", abstractFactor.getID());
		}
	}
	//
	// @Override
	// protected boolean isRelevantChange(StateChange value) {
	// return relevantChanges.contains(value);
	// }

	@Override
	protected Set<AbstractFactor> generateFactors(State state) {
		Set<AbstractFactor> factors = new HashSet<>();
		for (VariableID entityID : state.getEntityIDs()) {
			factors.add(new SingleVariableFactor(this, entityID));
		}
		return factors;
	}

}
