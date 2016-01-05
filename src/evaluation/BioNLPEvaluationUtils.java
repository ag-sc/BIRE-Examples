package evaluation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;

import corpus.FileUtils;
import corpus.LabeledDocument;
import corpus.SubDocument;
import corpus.Token;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.State;

public class BioNLPEvaluationUtils {

	private static Logger log = LogManager.getFormatterLogger();

	public BioNLPEvaluationUtils() {
	}

	public static boolean isEvent(EntityAnnotation e) {
		return e.getReadOnlyArguments().size() > 0;
	}

	private static String convertToEntityID(EntityAnnotation entity) {
		if (entity.isPriorKnowledge()) {
			/*
			 * use original IDs for prior knowledge
			 */
			return entity.getID().id;
		} else {
			/*
			 * prepend "T" for generated enitities so that scripts recognize
			 * this as a TextBound annotation.
			 */
			return "T-" + entity.getID().id;
		}
	}

	private static String convertToEventID(EntityAnnotation entity) {
		if (entity.isPriorKnowledge()) {
			/*
			 * use original IDs for prior knowledge
			 */
			return entity.getID().id;
		} else {
			/*
			 * prepend "E" for generated events so that scripts recognize this
			 * as a Event annotation annotation.
			 */
			return "E-" + entity.getID().id;
		}
	}

	public static String stateToBioNLPString(State state) {
		StringBuilder builder = new StringBuilder();
		for (EntityAnnotation e : state.getEntities()) {
			if (!isEvent(e)) {
				builder.append(entityToBioNLPString(e));
				builder.append("\n");
			} else {
				builder.append(eventToBioNLPString(e));
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	public static String entityToBioNLPString(EntityAnnotation e) {
		SubDocument doc = (SubDocument) e.getState().getDocument();
		List<Token> tokens = e.getTokens();
		String pattern = "%s\t%s %s %s\t%s";
		String convertedID = convertToEntityID(e);
		String type = e.getType().getName();
		String text = e.getText();
		int from = doc.getOffset() + tokens.get(0).getFrom();
		int to = doc.getOffset() + tokens.get(tokens.size() - 1).getTo();
		return String.format(pattern, convertedID, type, from, to, text);
	}

	public static String eventToBioNLPString(EntityAnnotation e) {
		SubDocument doc = (SubDocument) e.getState().getDocument();
		List<Token> tokens = e.getTokens();
		String triggerPattern = "%s\t%s %s %s\t%s";
		String eventPattern = "%s\t%s:%s";
		String argumentPattern = " %s:%s";
		String convertedID = convertToEventID(e);

		String type = e.getType().getName();
		String text = e.getText();
		int from = doc.getOffset() + tokens.get(0).getFrom();
		int to = doc.getOffset() + tokens.get(tokens.size() - 1).getTo();
		String triggerID = convertToEntityID(e);

		/*
		 * extract trigger part of this entity
		 */
		String trigger = String.format(triggerPattern, triggerID, type, from, to, text);
		/*
		 * extract event part of this entity
		 */
		String event = String.format(eventPattern, convertedID, type, triggerID);

		for (Entry<ArgumentRole, VariableID> arg : e.getReadOnlyArguments().entries()) {
			EntityAnnotation argEntity = e.getEntity(arg.getValue());
			String convertedArgID = null;
			if (isEvent(argEntity)) {
				convertedArgID = convertToEventID(argEntity);
			} else {
				convertedArgID = convertToEntityID(argEntity);
			}
			event += String.format(argumentPattern, arg.getKey().role, convertedArgID);
		}
		return trigger + "\n" + event;
	}

	/**
	 * Writes the given states to files that resemble the BioNLP annotation
	 * format. States the belong to the same original documents (that is, which
	 * relate to sentences in the same document) are written to the same
	 * annotation file. Inside the outputDir a new sub folder is created that is
	 * named with the current time stamp. The serialized files are stored in
	 * this sub folder.
	 *
	 * @param outputDir
	 * @param states
	 * @param wipeFolder
	 */
	public static Set<File> statesToBioNLPFiles(File outputDir, List<State> states, boolean wipeFolder) {
		Map<String, File> files = new HashMap<>();
		String newParentName = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss").format(new Date());
		File newParent = new File(outputDir, newParentName);
		newParent.mkdir();
		for (State s : states) {
			SubDocument d = (SubDocument) s.getDocument();
			File file = files.get(d.getParentDocumentName());
			if (file == null) {
				file = new File(newParent, d.getParentDocumentName() + ".a2");
				files.put(d.getParentDocumentName(), file);
			}
			try {
				String stateAsString = BioNLPEvaluationUtils.stateToBioNLPString(s);
				FileUtils.writeFile(file, stateAsString, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new HashSet<>(files.values());
	}

	public static F1Score evaluatePrediction(List<State> predictions, boolean ignoreRelations, boolean excludeTriggers,
			boolean excludePriors) {
		// log.debug("================================");
		F1Score score = new F1Score();
		for (State prediction : predictions) {
			State goldState = ((LabeledDocument<State, State>) prediction.getDocument()).getGoldResult();
			Predicate<EntityAnnotation> filter = e -> (!excludeTriggers || !isEvent(e))
					&& (!excludePriors || !e.isPriorKnowledge());

			/*
			 * Filter prior knowledge and trigger entities if requested.
			 */
			List<EntityAnnotation> predictedEntities = prediction.getEntities().stream().filter(filter)
					.collect(Collectors.toList());
			List<EntityAnnotation> correctEntities = goldState.getEntities().stream().filter(filter)
					.collect(Collectors.toList());

			int predictedMatches = 0;
			int predictedMismatches = 0;

			for (EntityAnnotation pe : predictedEntities) {
				boolean match = false;
				for (EntityAnnotation ce : correctEntities) {
					if (AnnotationUtils.matchEntities(pe, ce, ignoreRelations)) {
						match = true;
						break;
					}
				}
				if (match) {
					predictedMatches++;
				} else {
					predictedMismatches++;
				}
			}
			int correctMatches = 0;
			int correctMismatches = 0;

			for (EntityAnnotation ce : correctEntities) {
				boolean match = false;
				for (EntityAnnotation pe : predictedEntities) {
					if (AnnotationUtils.matchEntities(pe, ce, ignoreRelations)) {
						match = true;
						break;
					}
				}
				if (match) {
					correctMatches++;
				} else {
					correctMismatches++;
				}
			}

			if (predictedMatches != correctMatches) {
				log.warn("GOLD: %s", goldState);
				log.warn("PRED: %s", prediction);
				log.warn("predicted Matches = %s; correct Matches = %s", predictedMatches, correctMatches);
				log.warn("Golds:");
				for (EntityAnnotation entity : correctEntities) {
					log.warn("%s: %s %s-%s: \"%s\"", entity.getID(), entity.getType().getName(),
							entity.getBeginTokenIndex(), entity.getEndTokenIndex(), entity.getText());
				}
				log.warn("Preds:");
				for (EntityAnnotation entity : predictedEntities) {
					log.warn("%s: %s %s-%s: \"%s\"", entity.getID(), entity.getType().getName(),
							entity.getBeginTokenIndex(), entity.getEndTokenIndex(), entity.getText());
				}
			}

			// log.debug("--------------------------");
			// log.debug("G: %s", goldState);
			// log.debug("P: %s", prediction);
			// log.debug("TP: %s", predictedMatches);
			// log.debug("FP: %s", predictedMismatches);
			// log.debug("FN: %s", correctMismatches);
			score.tp += predictedMatches;
			score.fp += predictedMismatches;
			score.fn += correctMismatches;
		}
		score.score();
		return score;
	}

	public static F1Score evaluatePrediction2(List<State> predictions, boolean ignoreRelations, boolean excludeTriggers,
			boolean excludePriors) {
		F1Score score = new F1Score();
		for (State prediction : predictions) {
			State goldState = ((LabeledDocument<State, State>) prediction.getDocument()).getGoldResult();
			log.debug("GOLD       : %s", goldState);
			log.debug("PREDICTION : %s", prediction);

			Predicate<EntityAnnotation> filter = e -> (!excludeTriggers || !isEvent(e))
					&& (!excludePriors || !e.isPriorKnowledge());
			Function<EntityAnnotation, ?> map = e -> ignoreRelations ? new EntityComparisonWrapper(e)
					: new EventAndEntityComparisonWrapper(e);

			Set<?> correctEntities = goldState.getEntities().stream().filter(filter).map(map)
					.collect(Collectors.toSet());
			Set<?> predictedEntities = prediction.getEntities().stream().filter(filter).map(map)
					.collect(Collectors.toSet());

			int tp = Sets.intersection(correctEntities, predictedEntities).size();
			score.tp += tp;
			score.fp += predictedEntities.size() - tp;
			score.fn += correctEntities.size() - tp;
		}
		score.score();
		return score;
	}

	// public static void main(String[] args) {
	// EntityAnnotation e1 = new EntityAnnotation(null, "e1", new
	// EntityType("Type1"), 4, 7);
	// EntityAnnotation e2 = new EntityAnnotation(null, "e2", new
	// EntityType("Type1"), 4, 7);
	// EntityAnnotation e2b = new EntityAnnotation(null, "e2b", new
	// EntityType("Type2"), 4, 7);
	// EntityAnnotation e3 = new EntityAnnotation(null, "e3", new
	// EntityType("Type1"), 4, 8);
	// EntityAnnotation e4 = new EntityAnnotation(null, "e4", new
	// EntityType("Type2"), 4, 7);
	//
	// Multimap<ArgumentRole, VariableID> args1 = HashMultimap.create();
	// args1.put(new ArgumentRole("Role1"), new VariableID("e3"));
	// args1.put(new ArgumentRole("Role2"), new VariableID("e4"));
	//
	// Multimap<ArgumentRole, VariableID> args2 = HashMultimap.create();
	// args2.put(new ArgumentRole("Role1"), new VariableID("e3"));
	// args2.put(new ArgumentRole("Role2"), new VariableID("e4"));
	//
	// EntityAnnotation e5 = new EntityAnnotation(null, "e5", new
	// EntityType("Type3"), args1, 4, 7);
	// EntityAnnotation e6 = new EntityAnnotation(null, "e6", new
	// EntityType("Type2"), args2, 4, 7);
	//
	// List<EventAndEntityComparisonWrapper> entities = Arrays.asList(e1, e2,
	// e3, e2b, e4, e5, e6).stream()
	// .map(e -> new
	// EventAndEntityComparisonWrapper(e)).collect(Collectors.toList());
	// for (int i = 0; i < entities.size(); i++) {
	// EventAndEntityComparisonWrapper a = entities.get(i);
	// for (int j = i + 1; j < entities.size(); j++) {
	// EventAndEntityComparisonWrapper b = entities.get(j);
	// System.out.println(
	// String.format("%s %s %s", a.entity.getID(), a.equals(b) ? "==" : "!=",
	// b.entity.getID()));
	// }
	// }
	// }

	/**
	 * A wrapper object for an EntityAnnotation that overrides equals and
	 * hashCode such that only the boundaries and the entity type are compared.
	 * This object can be used in sets and HashMaps that should only consider
	 * these attributes of the annotation.
	 * 
	 * @author sjebbara
	 *
	 */
	static class EntityComparisonWrapper {
		public EntityAnnotation entity;

		public EntityComparisonWrapper(EntityAnnotation entity) {
			this.entity = entity;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + entity.getBeginTokenIndex();
			result = prime * result + entity.getEndTokenIndex();
			result = prime * result + ((entity.getType() == null) ? 0 : entity.getType().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EntityComparisonWrapper other = (EntityComparisonWrapper) obj;
			if (entity.getBeginTokenIndex() != other.entity.getBeginTokenIndex())
				return false;
			if (entity.getEndTokenIndex() != other.entity.getEndTokenIndex())
				return false;
			if (entity.getType() == null) {
				if (other.entity.getType() != null)
					return false;
			} else if (!entity.getType().equals(other.entity.getType()))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "EntityComparisonWrapper [entity=" + entity + "]";
		}
	}

	/**
	 * A wrapper object for an EntityAnnotation that overrides equals and
	 * hashCode such that only the boundaries, the entity type and the arguments
	 * are compared. This object can be used in sets and HashMaps that should
	 * only consider these attributes of the annotation.
	 * 
	 * @author sjebbara
	 *
	 */
	// FIXME The comparison of Arguments is not correct, since it only compares
	// the roles and IDs, where the IDs are almost never the same as in the
	// gold data.
	static class EventAndEntityComparisonWrapper {
		public EntityAnnotation entity;

		public EventAndEntityComparisonWrapper(EntityAnnotation entity) {
			this.entity = entity;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((entity.getReadOnlyArguments() == null) ? 0 : entity.getReadOnlyArguments().hashCode());
			result = prime * result + entity.getBeginTokenIndex();
			result = prime * result + entity.getEndTokenIndex();
			result = prime * result + ((entity.getType() == null) ? 0 : entity.getType().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EventAndEntityComparisonWrapper other = (EventAndEntityComparisonWrapper) obj;
			if (entity.getReadOnlyArguments() == null) {
				if (other.entity.getReadOnlyArguments() != null)
					return false;
			} else if (!entity.getReadOnlyArguments().equals(other.entity.getReadOnlyArguments()))
				return false;
			if (entity.getBeginTokenIndex() != other.entity.getBeginTokenIndex())
				return false;
			if (entity.getEndTokenIndex() != other.entity.getEndTokenIndex())
				return false;
			if (entity.getType() == null) {
				if (other.entity.getType() != null)
					return false;
			} else if (!entity.getType().equals(other.entity.getType()))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "EventAndEntityComparisonWrapper [entity=" + entity + "]";
		}
	}
}
