package corpus;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import corpus.Token;
import utility.VariableID;
import variables.EntityAnnotation;
import variables.State;

public class TFIDFStore implements Serializable {
	private static Logger log = LogManager.getFormatterLogger();

	private static final String NO_TYPE = "NONE";
	/**
	 * Rows: Terms, Columns: EntityType-Names
	 */
	public Table<String, String, Double> tfTable = HashBasedTable.create();
	public double numberOfDocuments = 0;

	public TFIDFStore(List<SubDocument> documents) {
		for (SubDocument document : documents) {
			State state = document.getGoldResult();
			for (Token token : document.getTokens()) {
				String text = token.getText();
				Set<VariableID> entities = state.getAnnotationsForToken(token);
				if (entities.isEmpty()) {
					Double value = tfTable.get(text, NO_TYPE);
					if (value == null) {
						value = new Double(0);
					}
					value++;
					tfTable.put(text, NO_TYPE, value);
				} else {
					for (VariableID variableID : entities) {
						EntityAnnotation entity = state.getEntity(variableID);
						Double value = tfTable.get(text, entity.getType().getName());
						if (value == null) {
							value = new Double(0);
						}
						value++;
						tfTable.put(text, entity.getType().getName(), value);
					}
				}
			}

			// for (EntityAnnotation entity : state.getEntities()) {
			// String type = entity.getType().getName();
			// String text = entity.getText();
			// Double value = tfTable.get(text, type);
			// if (value == null) {
			// value = new Double(0);
			// }
			// value++;
			// tfTable.put(text, type, value);
			// }
		}
		// for (String entityTypeName : tfTable.columnKeySet()) {
		// Map<String, Double> tfs = tfTable.column(entityTypeName);
		// double max = tfs.values().stream().reduce(0.0, Math::max);
		// /*
		// * Normalize with maximum frequency of document.
		// */
		// for (Entry<String, Double> termEntry : tfs.entrySet()) {
		// tfTable.put(termEntry.getKey(), entityTypeName, 0.5 + 0.5 *
		// termEntry.getValue() / max);
		// }
		// }
		numberOfDocuments = tfTable.columnKeySet().size();

	}

	public double getTFIDF(String term, String entityTypeName) {
		Double tf = this.tfTable.get(term, entityTypeName);
		if (tf == null || tf == 0) {
			return 0;
		}
		tf = Math.log(tf + 1);
		double idf = Math.log(numberOfDocuments / (tfTable.row(term).size()));
		double tfidf = tf * idf;
		return tfidf;
	}

}
