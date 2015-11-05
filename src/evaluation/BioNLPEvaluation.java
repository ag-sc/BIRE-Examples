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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;

import corpus.SubDocument;
import corpus.Token;
import corpus.parser.FileUtils;
import corpus.parser.bionlp.julie.Tokenization;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.State;

public class BioNLPEvaluation {

	private static Logger log = LogManager.getFormatterLogger(Tokenization.class.getName());

	public BioNLPEvaluation() {
	}

	// public final static Set<String> entities = new
	// HashSet<>(Arrays.asList("Protein", "Entity"));
	// public final static Set<String> events = new HashSet<>(
	// Arrays.asList("Gene_expression", "Transcription", "Protein_catabolism",
	// "Localization", "Binding",
	// "Phosphorylation", "Regulation", "Positive_regulation",
	// "Negative_regulation"));

	public static boolean isEvent(EntityAnnotation e) {
		return e.getReadOnlyArguments().size() > 0;
	}

	private static String convertToEntityID(String rawID) {
		return "T-" + rawID;
	}

	private static String convertToEventID(String rawID) {
		return "E-" + rawID;
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
		String rawID = e.getID().id;
		String convertedID = convertToEntityID(rawID);
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
		String rawID = e.getID().id;
		String convertedID = convertToEventID(rawID);

		String type = e.getType().getName();
		String text = e.getText();
		int from = doc.getOffset() + tokens.get(0).getFrom();
		int to = doc.getOffset() + tokens.get(tokens.size() - 1).getTo();
		String triggerID = convertToEntityID(rawID);

		String trigger = String.format(triggerPattern, triggerID, type, from, to, text);
		String event = String.format(eventPattern, convertedID, type, triggerID);

		for (Entry<ArgumentRole, VariableID> arg : e.getReadOnlyArguments().entries()) {
			EntityAnnotation argEntity = e.getEntity(arg.getValue());
			String convertedArgID = null;
			if (isEvent(argEntity)) {
				convertedArgID = convertToEventID(arg.getValue().id);
			} else {
				convertedArgID = convertToEntityID(arg.getValue().id);
			}
			event += String.format(argumentPattern, arg.getKey().role, convertedArgID);
		}
		return trigger + "\n" + event;
	}

	public static double strictEquality(State state, State goldState) {
		Collection<EntityAnnotation> entities = state.getEntities();
		Collection<EntityAnnotation> goldEntities = goldState.getEntities();
		double tpGold = 0;
		double tpPredicted = 0;
		double fp = 0;
		double fn = 0;

		for (EntityAnnotation goldEntity : goldEntities) {
			boolean match = false;
			for (EntityAnnotation entity : entities) {
				match = matchEntities(entity, goldEntity);
				if (match)
					break;
			}
			if (match)
				tpGold++;
			else
				fn++;
		}

		for (EntityAnnotation entity : entities) {
			boolean match = false;
			for (EntityAnnotation goldEntity : goldEntities) {
				match = matchEntities(entity, goldEntity);
				if (match)
					break;
			}
			if (match)
				tpPredicted++;
			else
				fp++;
		}
		/*
		 * Count true positives separately and use the minimum. This is needed
		 * in the case, where we have multiple identical entities in one state
		 * that all match a single entity in the other. All of these identical
		 * entities would increase the true positives without, affecting false
		 * positives/negatives. An alternative would be to eliminate duplicates
		 * prior to measuring.
		 */

		double tp = Math.min(tpGold, tpPredicted);
		double precision = tp / (tp + fp);
		double recall = tp / (tp + fn);
		double f1 = 2 * (precision * recall) / (precision + recall);
		log.debug("TP: %s, FP: %s, FN: %s, P: %s, R: %s, F1: %s", tp, fp, fn, precision, recall, f1);
		return f1;
	}

	// private static Collection<AEntityAnnotation>
	// unique(Collection<AEntityAnnotation> entities) {
	// Collection<AEntityAnnotation> uniqueEntities = new
	// ArrayList<AEntityAnnotation>();
	// for (AEntityAnnotation entity : entities) {
	// boolean match = false;
	// for (EntityAnnotation uEntity : uniqueEntities) {
	// match = matchEntities(entity, uEntity);
	// if (match)
	// break;
	// }
	// if (match)
	// uniqueEntities.add(entity);
	// }
	// return uniqueEntities;
	// }

	/**
	 * True, if these two entities match, false otherwise, given the strict
	 * equality defined at:
	 * http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/evaluation.shtml
	 * 
	 * 
	 * @param e1
	 * @param e2
	 * @return
	 */
	private static boolean matchEntities(EntityAnnotation e1, EntityAnnotation e2) {
		if (!e1.getType().getName().equals(e2.getType().getName()))
			return false;
		if (e1.getBeginTokenIndex() != e2.getBeginTokenIndex() || e1.getEndTokenIndex() != e2.getEndTokenIndex())
			return false;
		if (!matchArguments(e1, e2))
			return false;
		return true;

	}

	/**
	 * True, if the arguments of both given entities match each other, false
	 * otherwise. More detail at:
	 * http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/evaluation.shtml
	 * 
	 * @param e1
	 * @param e2
	 * @return
	 */
	private static boolean matchArguments(EntityAnnotation e1, EntityAnnotation e2) {
		Multimap<ArgumentRole, VariableID> arguments1 = e1.getReadOnlyArguments();
		Multimap<ArgumentRole, VariableID> arguments2 = e2.getReadOnlyArguments();
		// this is a fast-reject test
		if (arguments1.size() != arguments2.size())
			return false;
		for (Entry<ArgumentRole, VariableID> argument1 : arguments1.entries()) {
			if (!containsArgument(e2, arguments2, e1, argument1))
				return false;
		}
		for (Entry<ArgumentRole, VariableID> argument2 : arguments2.entries()) {
			if (!containsArgument(e1, arguments1, e2, argument2))
				return false;
		}
		return true;
	}

	/**
	 * True, if the given Map of arguments <i>arguments1</i> of entity <i>e1</i>
	 * contains an argument that matches the argument <i>argument2</i> of entity
	 * <i>e2</i>, false otherwise. More detail at:
	 * http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/evaluation.shtml
	 * 
	 * @param e1
	 * @param arguments1
	 * @param e2
	 * @param argument2
	 * @return
	 */
	private static boolean containsArgument(EntityAnnotation e1, Multimap<ArgumentRole, VariableID> arguments1,
			EntityAnnotation e2, Entry<ArgumentRole, VariableID> argument2) {
		Collection<VariableID> possibleMatches = arguments1.get(argument2.getKey());
		for (VariableID entityID : possibleMatches) {
			if (matchEntities(e1.getEntity(entityID), e2.getEntity(argument2.getValue())))
				return true;
		}
		return false;
	}

	// /**
	// * Writes the given states to files that resemble the BioNLP annotation
	// * format. States the belong to the same original documents (that is,
	// which
	// * relate to sentences in the same document) are written to the same
	// * annotation file. If the wipeFolder flag is set to true, the user is
	// * prompted for confirmation and the contents of the outputDir folder are
	// * deleted before writing the state files.
	// *
	// * @param outputDir
	// * @param states
	// * @param wipeFolder
	// */
	public static Set<File> statesToBioNLPFiles(File outputDir, List<State> states, boolean wipeFolder) {
		// if (wipeFolder) {
		// // int userInput = JOptionPane.showConfirmDialog(null,
		// // String.format("Really wipe folder \"%s\" before writing states?",
		// // outputDir.getPath()));
		// // if (userInput == JOptionPane.OK_OPTION) {
		// for (File f : outputDir.listFiles()) {
		// f.delete();
		// }
		// // }
		// }
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
				String stateAsString = BioNLPEvaluation.stateToBioNLPString(s);
				FileUtils.writeFile(file, stateAsString, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new HashSet<>(files.values());
	}
}
