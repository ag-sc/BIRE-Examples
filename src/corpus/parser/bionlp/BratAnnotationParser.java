package corpus.parser.bionlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import corpus.FileUtils;
import corpus.parser.bionlp.annotations.BratAnnotation;
import corpus.parser.bionlp.annotations.BratAttributeAnnotation;
import corpus.parser.bionlp.annotations.BratEventAnnotation;
import corpus.parser.bionlp.annotations.BratRelationAnnotation;
import corpus.parser.bionlp.annotations.BratTextBoundAnnotation;
import utility.ID;

public class BratAnnotationParser {
	private static Logger log = LogManager.getFormatterLogger();
	private static final String COMMENT_INDICATOR = "#";
	private static final String UNKNOWN_ANNOTATION_TYPE = "Line %s was not recognized as a supported annotation:\n"
			+ "\t%s";

	BratAnnotationManager manager = new BratAnnotationManager();

	/*
	 * Collect all entity/relation types and their frequencies save to separate
	 * file
	 */
	public BratAnnotatedDocument parseFile(File textFile, List<File> annFiles) {
		try {
			for (File annFile : annFiles) {
				BufferedReader annotationReader = new BufferedReader(new FileReader(annFile));
				String line;
				int lineNumber = 0;
				while ((line = annotationReader.readLine()) != null) {
					if (line.startsWith(COMMENT_INDICATOR)) {
						log.warn("Skip comment: \"%s\"", line);
					} else {
						BratAnnotation annotation = parseLine(line, lineNumber);
						if (annotation != null) {
							manager.addAnnotation(annFile.getName(), annotation);
						}
					}
					lineNumber++;
				}
				annotationReader.close();
			}

			// Read Text
			String content = FileUtils.readFile(textFile);
			BratAnnotatedDocument doc = new BratAnnotatedDocument(
					FileUtils.getFilenameWithoutExtension(textFile.getName()), content, manager);
			return doc;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Delegates the parsing to the appropriate method that is capable of
	 * parsing this specific type of annotation. The decision to which method
	 * this line is delegated is made with the {@link Pattern} object in the
	 * BratAnnotation-Objects.
	 * 
	 * @param line
	 */
	private BratAnnotation parseLine(String line, int lineNumber) {
		Matcher textMatcher = BratTextBoundAnnotation.pattern.matcher(line);
		if (textMatcher.matches()) {
			return parseTextBoundAnnotation(line);
		}
		Matcher eventMatcher = BratEventAnnotation.pattern.matcher(line);
		if (eventMatcher.matches()) {
			return parseEventAnnotation(line);
		}
		Matcher relationMatcher = BratRelationAnnotation.pattern.matcher(line);
		if (relationMatcher.matches()) {
			return parseRelationAnnotation(line);
		}
		Matcher attributeMatcher = BratAttributeAnnotation.pattern.matcher(line);
		if (attributeMatcher.matches()) {
			return parseAttributeAnnotation(line);
		}
		log.warn(UNKNOWN_ANNOTATION_TYPE, lineNumber, line);
		return null;
	}

	private BratTextBoundAnnotation parseTextBoundAnnotation(String line) {
		StringTokenizer tabTokenizer = new StringTokenizer(line, "\t");

		String id = tabTokenizer.nextToken();

		StringTokenizer spaceTokenizer = new StringTokenizer(tabTokenizer.nextToken());
		String role = spaceTokenizer.nextToken();
		String start = spaceTokenizer.nextToken();
		String end = spaceTokenizer.nextToken();

		String text = tabTokenizer.nextToken();

		BratTextBoundAnnotation annotation = new BratTextBoundAnnotation(manager, id, role, Integer.parseInt(start),
				Integer.parseInt(end), text);
		return annotation;
	}

	private BratEventAnnotation parseEventAnnotation(String line) {
		StringTokenizer tokenizer = new StringTokenizer(line);

		String id = tokenizer.nextToken();
		String[] split = tokenizer.nextToken().split(":");
		String role = split[0];
		String triggerID = split[1];

		Multimap<String, ID<? extends BratAnnotation>> arguments = extractArgumentsAsMap(tokenizer);

		BratEventAnnotation annotation = new BratEventAnnotation(manager, id, role, triggerID, arguments);
		return annotation;
	}

	private BratRelationAnnotation parseRelationAnnotation(String line) {
		StringTokenizer tokenizer = new StringTokenizer(line);

		String id = tokenizer.nextToken();
		String role = tokenizer.nextToken();
		Multimap<String, ID<? extends BratAnnotation>> arguments = extractArgumentsAsMap(tokenizer);

		BratRelationAnnotation annotation = new BratRelationAnnotation(manager, id, role, arguments);
		return annotation;
	}

	private BratAttributeAnnotation parseAttributeAnnotation(String line) {
		StringTokenizer tokenizer = new StringTokenizer(line);

		String id = tokenizer.nextToken();
		String role = tokenizer.nextToken();
		List<ID<? extends BratAnnotation>> arguments = new ArrayList<>();
		while (tokenizer.hasMoreElements()) {
			String argumentID = tokenizer.nextToken();
			arguments.add(new ID<>(argumentID));
		}

		BratAttributeAnnotation annotation = new BratAttributeAnnotation(manager, id, role, arguments);
		return annotation;
	}

	private Multimap<String, ID<? extends BratAnnotation>> extractArgumentsAsMap(StringTokenizer tokenizer) {
		Multimap<String, ID<? extends BratAnnotation>> arguments = HashMultimap.create();
		while (tokenizer.hasMoreElements()) {
			String[] split = tokenizer.nextToken().split(":");
			arguments.put(split[0], new ID<>(split[1]));
		}
		return arguments;
	}

}
