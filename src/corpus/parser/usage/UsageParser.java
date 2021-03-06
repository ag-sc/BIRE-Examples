package corpus.parser.usage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import corpus.AnnotationConfig;
import corpus.DefaultCorpus;
import corpus.EntityTypeDefinition;
import corpus.LabeledDocument;
import corpus.Token;
import corpus.parser.ParsingUtils;
import utility.VariableID;
import variables.Argument;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.EntityType;
import variables.State;

public class UsageParser {
	private static Logger log = LogManager.getFormatterLogger();
	private static final String SUBJECTIVE_TYPE_NAME = "SUBJECTIVE";
	private static final String POSITIVE_SUBJECTIVE_TYPE_NAME = "POSITIVE-SUBJECTIVE";
	private static final String NEUTRAL_SUBJECTIVE_TYPE_NAME = "NEUTRAL-SUBJECTIVE";
	private static final String NEGATIVE_SUBJECTIVE_TYPE_NAME = "NEGATIVE-SUBJECTIVE";
	private static final String UNKNOWN_SUBJECTIVE_TYPE_NAME = "UNKNOWN-SUBJECTIVE";

	private static final String ASPECT_TYPE_NAME = "ASPECT";

	private static final String TARGET_SUBJECTIVE_TYPE_NAME = "TARG-SUBJ";

	private static final String COREFERENCE_TYPE_NAME = "COREF";

	private static final ArgumentRole ASPECT_ROLE = new ArgumentRole("target");
	// private static final String SUBJECTIVE_ROLE_NAME = "subjective";

	private static final ArgumentRole REFERENT_ROLE = new ArgumentRole("referent");
	// private static final String COREFERENCE_ROLE_NAME = "coreference";

	private static final String ANNOTATOR_ID_1 = "a1";
	private static final String ANNOTATOR_ID_2 = "a2";

	public static void main(String[] args) {
		File annDir = new File("res/usage/de");

		DefaultCorpus<LabeledDocument<State, State>> corpus = parseCorpus(annDir);
		log.debug("Corpus: %s", corpus.toDetailedString());
	}

	public static DefaultCorpus<LabeledDocument<State, State>> parseCorpus(File annDir) {
		File[] allFiles = annDir.listFiles();

		Map<String, File> textFiles = new HashMap<String, File>();
		Map<String, File> annotationFilesA1 = new HashMap<String, File>();
		Map<String, File> annotationFilesA2 = new HashMap<String, File>();
		Map<String, File> relationFilesA1 = new HashMap<String, File>();
		Map<String, File> relationFilesA2 = new HashMap<String, File>();

		for (File file : allFiles) {
			String[] nameSplit = file.getName().split("\\.");
			switch (nameSplit[1]) {
			case "txt": {
				String category = nameSplit[0];
				textFiles.put(category, file);
				break;
			}
			case "csv": {
				String tmpName = nameSplit[0];
				String category = tmpName.substring(0, tmpName.length() - 3);
				String annotatorID = tmpName.substring(tmpName.length() - 2);
				if (ANNOTATOR_ID_1.equals(annotatorID)) {
					annotationFilesA1.put(category, file);
				} else if (ANNOTATOR_ID_2.equals(annotatorID)) {
					annotationFilesA2.put(category, file);
				}
				break;
			}
			case "rel": {
				String tmpCategory = nameSplit[0];
				String category = tmpCategory.substring(0, tmpCategory.length() - 3);
				String annotatorID = tmpCategory.substring(tmpCategory.length() - 2);
				if (ANNOTATOR_ID_1.equals(annotatorID)) {
					relationFilesA1.put(category, file);
				} else if (ANNOTATOR_ID_2.equals(annotatorID)) {
					relationFilesA2.put(category, file);
				}
				break;
			}
			default:
				break;
			}
		}

		AnnotationConfig config = getUsageConfig();
		DefaultCorpus<LabeledDocument<State, State>> corpus = new DefaultCorpus<>(config);

		for (String category : textFiles.keySet()) {
			corpus.addDocuments(parseFile(corpus, category, textFiles.get(category), annotationFilesA1.get(category),
					relationFilesA1.get(category)));
			corpus.addDocuments(parseFile(corpus, category, textFiles.get(category), annotationFilesA2.get(category),
					relationFilesA2.get(category)));
		}
		return corpus;
	}

	private static Collection<LabeledDocument<State, State>> parseFile(
			DefaultCorpus<LabeledDocument<State, State>> corpus, String category, File t, File a1, File r1) {
		log.debug("Process category %s", category);
		Map<String, LabeledDocument<State, State>> documents = parseDocument(corpus, category, t);

		addEntities(corpus, a1, documents);
		addRelations(corpus, r1, documents);

		return documents.values();
	}

	private static Map<String, LabeledDocument<State, State>> parseDocument(
			DefaultCorpus<LabeledDocument<State, State>> corpus, String category, File t) {
		Map<String, LabeledDocument<State, State>> documents = new HashMap<>();
		try {
			FileInputStream fileStream = new FileInputStream(t);
			InputStreamReader streamReader = new InputStreamReader(fileStream, "UTF-8");
			BufferedReader textReader = new BufferedReader(streamReader);
			String line;
			while ((line = textReader.readLine()) != null) {
				String[] columns = line.split("\t");
				String documentID = columns[0];
				String productID = columns[1];
				String reviewID = columns[2];
				String productName = columns[3];
				String title = columns[4];
				String content = columns[5];
				content = title + " " + content;

				List<Token> tokens = tokenize(content);

				LabeledDocument<State, State> doc = new LabeledDocument<>(
						String.format("%s-%s-%s-%s", category, documentID, productID, reviewID), content, tokens);
				doc.setPriorKnowledge(new State(doc));
				doc.setGoldResult(new State(doc));
				documents.put(documentID, doc);
			}
			textReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return documents;
	}

	private static void addEntities(DefaultCorpus<LabeledDocument<State, State>> corpus, File annotationFile,
			Map<String, LabeledDocument<State, State>> documents) {

		try {
			FileInputStream fileStream = new FileInputStream(annotationFile);
			InputStreamReader streamReader = new InputStreamReader(fileStream, "UTF-8");
			BufferedReader annotationReader = new BufferedReader(streamReader);
			String line;
			int lineNumber = 0;
			while ((line = annotationReader.readLine()) != null) {
				String[] columns = line.split("\t");
				String entityTypeName = columns[0].toUpperCase();
				String documentID = columns[1];
				int from = Integer.parseInt(columns[2]);
				int to = Integer.parseInt(columns[3]);
				String text = columns[4];
				String entityID = columns[5];
				String subjectivity = columns[6];
				String relatedness = columns[7];
				LabeledDocument<State, State> doc = documents.get(documentID);
				State goldState = doc.getGoldResult();

				if (SUBJECTIVE_TYPE_NAME.equals(entityTypeName)) {
					/**
					 * If the parsed line stores an annotation of type
					 * "subjective", add the subjectivity prefix.
					 */
					entityTypeName = subjectivity.toUpperCase() + "-" + entityTypeName;
				}

				EntityType entityType = new EntityType(entityTypeName);
				if (entityType == null) {
					log.warn("EnitityType %s for annotation \"%s\" in file %s and line %s not found in given config.",
							entityTypeName, text, annotationFile, lineNumber);
				}

				// FIXME check if span search is correct for Usage annotations
				int beginTokenIndex = ParsingUtils.binarySpanSearch(doc.getTokens(), from, true);
				if (beginTokenIndex == -1) {
					log.warn(
							"No (begin) token found for character position %s for annotation \"%s\" in file %s and line %s.",
							from, text, annotationFile, lineNumber);
					log.debug("Tokens: %s", doc.getTokens());
				}
				int endTokenIndex = ParsingUtils.binarySpanSearch(doc.getTokens(), to, false);
				if (endTokenIndex == -1) {
					log.warn(
							"No (end) token found for character position %s for annotation \"%s\" in file %s and line %s.",
							to, text, annotationFile, lineNumber);
					log.debug("Tokens: %s", doc.getTokens());
				}

				EntityAnnotation e = new EntityAnnotation(goldState, entityID, entityType, beginTokenIndex,
						endTokenIndex);
				goldState.addEntity(e);
				lineNumber++;
			}
			annotationReader.close();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void addRelations(DefaultCorpus<LabeledDocument<State, State>> corpus, File relationFile,
			Map<String, LabeledDocument<State, State>> documents) {

		try {
			FileInputStream fileStream = new FileInputStream(relationFile);
			InputStreamReader streamReader = new InputStreamReader(fileStream, "UTF-8");
			BufferedReader relationReader = new BufferedReader(streamReader);
			String line;
			int lineNumber = 0;
			while ((line = relationReader.readLine()) != null) {
				String[] columns = line.split("\t");
				String entityTypeName = columns[0];
				String documentID = columns[1];
				String argument1ID = columns[2];
				String argument2ID = columns[3];

				LabeledDocument<State, State> doc = documents.get(documentID);
				State goldState = doc.getGoldResult();
				EntityType entityType = new EntityType(entityTypeName);
				if (entityType == null) {
					log.warn("EnitityType \"%s\" for annotation in file %s and line %s not found in given config.",
							entityTypeName, relationFile, lineNumber);
				}
				Multimap<ArgumentRole, VariableID> arguments = HashMultimap.create();
				String triggerID = null;
				if (TARGET_SUBJECTIVE_TYPE_NAME.equals(entityTypeName)) {
					/**
					 * We define that the trigger for the target-subjective
					 * relation is the subjective phrase (second argument). The
					 * aspect is then considered as the target.
					 */
					triggerID = argument2ID;
					arguments.put(ASPECT_ROLE, new VariableID(argument1ID));
				} else if (COREFERENCE_TYPE_NAME.equals(entityTypeName)) {
					/**
					 * We define that the trigger for a coreference relation is
					 * the coreferent phrase.
					 */
					triggerID = argument2ID;
					arguments.put(REFERENT_ROLE, new VariableID(argument1ID));
				} else {
					log.warn("Unexpected relation type \"%s\" found in file %s and line %s.", entityTypeName,
							relationFile, lineNumber);
				}

				EntityAnnotation trigger = goldState.getEntity(new VariableID(triggerID));

				if (trigger == null) {
					log.warn("No (trigger) entity found for id \"%s\" in file %s and line %s.", triggerID, relationFile,
							lineNumber);
				}

				EntityAnnotation e = new EntityAnnotation(goldState, entityType, arguments,
						trigger.getBeginTokenIndex(), trigger.getEndTokenIndex());
				goldState.addEntity(e);
				lineNumber++;
			}
			relationReader.close();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<Token> tokenize(String content) {
		log.debug("Text (%s): %s", content.length(), content);
		List<Token> tokens = new ArrayList<Token>();
		Locale currentLocale = Locale.GERMANY;

		BreakIterator wordIterator = BreakIterator.getWordInstance(currentLocale);

		wordIterator.setText(content);
		int index = 0;
		int from = wordIterator.first();
		int to = 0;
		while ((to = wordIterator.next()) != BreakIterator.DONE) {
			String text = content.substring(from, to);
			if (!text.matches("\\s+")) {
				log.debug("%s: %s - %s: %s", index, from, to, text);
				Token token = new Token(index, from, to, text);
				tokens.add(token);
				index++;
			} else {
				log.debug("%s: %s - %s: %s\t(Whitespace only. Skip.)", index, from, to, text);
			}
			from = to;
		}
		// Pattern p = Pattern.compile("\\S+");
		// Matcher m = p.matcher(content);
		// int index = 0;
		// while (m.find()) {
		// int from = m.start();
		// int to = m.end();
		// String text = m.group();
		// log.debug("%s: %s", index, text);
		// Token token = new Token(index, from, to, text);
		// tokens.add(token);
		// }

		return tokens;
	}

	private static AnnotationConfig getUsageConfig() {
		/**
		 * The configuration is not parsed from the given files. Hand-crafting
		 * it in code is much easier than parsing it. We convert the binary
		 * relations "TARG-SUBJ" and "COREF" to unary relations, by regarding
		 * one of the arguments as a trigger.
		 */
		AnnotationConfig config = new AnnotationConfig();
		EntityTypeDefinition aspectType = new EntityTypeDefinition(ASPECT_TYPE_NAME);
		EntityTypeDefinition positiveSubjectiveType = new EntityTypeDefinition(POSITIVE_SUBJECTIVE_TYPE_NAME);
		EntityTypeDefinition neutralSubjectiveType = new EntityTypeDefinition(NEUTRAL_SUBJECTIVE_TYPE_NAME);
		EntityTypeDefinition negativeSubjectiveType = new EntityTypeDefinition(NEGATIVE_SUBJECTIVE_TYPE_NAME);
		EntityTypeDefinition unknownSubjectiveType = new EntityTypeDefinition(UNKNOWN_SUBJECTIVE_TYPE_NAME);

		Map<ArgumentRole, Argument> targetSubjectiveCoreArguments = new HashMap<>();
		targetSubjectiveCoreArguments.put(ASPECT_ROLE, new Argument(ASPECT_ROLE, Arrays.asList(ASPECT_TYPE_NAME)));
		// targetSubjectiveCoreArguments.put(
		// SUBJECTIVE_ROLE_NAME,
		// new Argument(SUBJECTIVE_ROLE_NAME, Arrays.asList(
		// POSITIVE_SUBJECTIVE_TYPE_NAME,
		// NEUTRAL_SUBJECTIVE_TYPE_NAME,
		// NEGATIVE_SUBJECTIVE_TYPE_NAME)));
		EntityTypeDefinition targetSubjectiveRelationType = new EntityTypeDefinition(TARGET_SUBJECTIVE_TYPE_NAME,
				targetSubjectiveCoreArguments, new HashMap<ArgumentRole, Argument>());

		Map<ArgumentRole, Argument> coreferenceCoreArguments = new HashMap<>();
		coreferenceCoreArguments.put(REFERENT_ROLE, new Argument(REFERENT_ROLE, Arrays.asList(ASPECT_TYPE_NAME)));
		// coreferenceCoreArguments.put(COREFERENCE_ROLE_NAME, new Argument(
		// COREFERENCE_ROLE_NAME, Arrays.asList(ASPECT_TYPE_NAME)));
		EntityTypeDefinition coreferenceRelationType = new EntityTypeDefinition(COREFERENCE_TYPE_NAME,
				coreferenceCoreArguments, new HashMap<ArgumentRole, Argument>());

		config.addEntityType(aspectType);
		config.addEntityType(positiveSubjectiveType);
		config.addEntityType(neutralSubjectiveType);
		config.addEntityType(negativeSubjectiveType);
		config.addEntityType(unknownSubjectiveType);
		config.addEntityType(targetSubjectiveRelationType);
		config.addEntityType(coreferenceRelationType);
		return config;
	}
}
