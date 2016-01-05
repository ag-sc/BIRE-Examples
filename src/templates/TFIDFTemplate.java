package templates;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.TFIDFStore;
import corpus.Token;
import factors.AbstractFactor;
import factors.impl.SingleVariableFactor;
import learning.Vector;
import utility.VariableID;
import variables.EntityAnnotation;
import variables.State;

public class TFIDFTemplate extends AbstractTemplate<State> {
	private static Logger log = LogManager.getFormatterLogger(EntityTemplate.class.getName());

	private TFIDFStore store;

	public TFIDFTemplate(TFIDFStore store) {
		this.store = store;
	}

	@Override
	public void computeFactor(State state, AbstractFactor abstractFactor) {
		if (abstractFactor instanceof SingleVariableFactor) {

			SingleVariableFactor factor = (SingleVariableFactor) abstractFactor;
			EntityAnnotation entity = state.getEntity(factor.entityID);
			log.debug("%s: Add features to entity %s (\"%s\"):", this.getClass().getSimpleName(), entity.getID(),
					entity.getText());
			Vector featureVector = new Vector();

			String entityTypePrefix = "ENTITY_TYPE=" + entity.getType().getName() + "_";
			double max = 0;
			double avrg = 0;
			double sum = 0;
			double min = 0;
			double prod = 1;
			int inTypeDict = 0;

			for (Token token : entity.getTokens()) {
				double tfidf = store.getTFIDF(token.getText(), entity.getType().getName());
				if (tfidf > max)
					max = tfidf;
				if (tfidf < min)
					min = tfidf;
				sum += tfidf;

				prod *= (tfidf);
				inTypeDict += store.tfTable.contains(token.getText(), entity.getType().getName()) ? 1 : 0;
				// inTypeDict += store.tfidfTable.containsRow(token.getText()) ?
				// 1 : 0;
			}
			avrg = sum / entity.getTokens().size();

			boolean all = entity.getTokens().size() == inTypeDict;
			boolean any = inTypeDict > 0;
			boolean none = inTypeDict == 0;
			double ratio = inTypeDict / entity.getTokens().size();

			// featureVector.set(entityTypePrefix + "MAX_TF-IDF_OF_TOKENS",
			// max);
			// featureVector.set(entityTypePrefix + "MIN_TF-IDF_OF_TOKENS",
			// min);
			featureVector.set(entityTypePrefix + "AVRG_TF-IDF_OF_TOKENS", avrg);
			featureVector.set(entityTypePrefix + "SUM_TF-IDF_OF_TOKENS", sum);
			// featureVector.set(entityTypePrefix + "PROD_TF-IDF_OF_TOKENS",
			// prod);

			featureVector.set(entityTypePrefix + "ALL_TOKENS_IN_DICT", all);
			featureVector.set(entityTypePrefix + "ANY_TOKENS_IN_DICT", any);
			featureVector.set(entityTypePrefix + "NO_TOKEN_IN_DICT", none);

			// boolean textInDict = store.tfTable.containsRow(entity.getText());
			// featureVector.set(entityTypePrefix + "TEXT_IN_DICT", textInDict);

			featureVector.set(
					entityTypePrefix + String.format("%s/%s_TOKEN_IN_TYPE_DICT", inTypeDict, entity.getTokens().size()),
					1.);

			// featureVector.set(entityTypePrefix +
			// String.format("%s_TOKEN_IN_TYPE_DICT", inTypeDict), 1.);
			// featureVector.set(entityTypePrefix +

			// "#TOKEN_IN_TYPE_DICT_RATIO>0.2", ratio > 0.2);
			// featureVector.set(entityTypePrefix +
			// "#TOKEN_IN_TYPE_DICT_RATIO>0.5", ratio > 0.5);
			// featureVector.set(entityTypePrefix +
			// "#TOKEN_IN_TYPE_DICT_RATIO>0.7", ratio > 0.7);
			// featureVector.set(entityTypePrefix +
			// "#TOKEN_IN_TYPE_DICT_RATIO>0.9", ratio > 0.9);
			//
			// featureVector.set(entityTypePrefix +
			// "#TOKEN_IN_TYPE_DICT_RATIO<=0.2", ratio <= 0.2);
			// featureVector.set(entityTypePrefix +
			// "#TOKEN_IN_TYPE_DICT_RATIO<=0.5", ratio <= 0.5);
			// featureVector.set(entityTypePrefix +
			// "#TOKEN_IN_TYPE_DICT_RATIO<=0.7", ratio <= 0.7);
			// featureVector.set(entityTypePrefix +
			// "#TOKEN_IN_TYPE_DICT_RATIO<=0.9", ratio <= 0.9);

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

	public double dropoutProbability = 0;
}
