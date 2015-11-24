package evaluation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.FileUtils;
import corpus.SubDocument;
import corpus.Token;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.State;

public class BioNLPEvaluation {

	private static Logger log = LogManager.getFormatterLogger(BioNLPEvaluation.class.getName());

	public BioNLPEvaluation() {
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
				String stateAsString = BioNLPEvaluation.stateToBioNLPString(s);
				FileUtils.writeFile(file, stateAsString, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new HashSet<>(files.values());
	}
}
