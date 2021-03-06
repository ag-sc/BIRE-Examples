package corpus.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.FileUtils;
import corpus.parser.bionlp.BratAnnotatedDocument;
import corpus.parser.bionlp.annotations.BratAnnotation;
import corpus.parser.bionlp.annotations.BratTextBoundAnnotation;

public class JavaSentenceSplitter {

	private static Logger log = LogManager.getFormatterLogger(Tokenization.class.getName());

	public static void main(String[] args) {
		List<String> sentences;
		try {
			sentences = getSentencesAsList(new File(
					"/homes/sjebbara/datasets/BioNLP-ST-2013-GE/train/PMC-1310901-03-MATERIALS_AND_METHODS.txt"));
			sentences.forEach(s -> System.out.println(String.format("(%s): %s", s.length(), s)));
			int sum = 0;
			for (String string : sentences) {
				sum += string.length();
			}
			System.out.println(sum);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static List<String> getSentencesAsList(File srcFile) throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(srcFile));
		String line;
		List<String> sentences = new ArrayList<String>();
		while ((line = reader.readLine()) != null) {
			BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
			iterator.setText(line);
			int start = iterator.first();
			for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
				// OBSOLETE end - 1 to remove the trailing whitespace
				String sentence = line.substring(start, end);
				sentences.add(sentence);
				log.debug(sentence);
			}

			/*
			 * This little hack with the trailing whitespaces is necessary so
			 * that the character offsets of the respective annotations stay
			 * consistent with the sentence-splitted text. Without it, the
			 * linebreak at the end of each line is neglected.
			 */
			String sentence = sentences.get(sentences.size() - 1);
			sentence = sentence + "\n";
			sentences.set(sentences.size() - 1, sentence);
		}
		reader.close();
		return sentences;
	}

	public static String getSentencesAsString(File srcFile) throws IOException {
		List<String> sentences = getSentencesAsList(srcFile);

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < sentences.size(); i++) {
			builder.append(sentences.get(i));
			if (i < sentences.size() - 1)
				builder.append("\n");
		}

		String splittedText = builder.toString();
		return splittedText;
	}

	public static void extractAndStoreSentences(File srcFile, File sentFile) throws IOException {
		String splittedText = getSentencesAsString(srcFile);
		FileUtils.writeFile(sentFile, splittedText);
	}

	public static boolean isConsistent(String original, String splitted, BratAnnotatedDocument doc) {
		boolean isConsistent = true;
		for (BratAnnotation ann : doc.getAllAnnotations().values()) {
			if (ann instanceof BratTextBoundAnnotation) {
				BratTextBoundAnnotation t = (BratTextBoundAnnotation) ann;
				int beginIndex = t.getStart();
				int endIndex = t.getEnd();

				String originalAnnText = original.substring(beginIndex, endIndex);
				String splittedAnnText = splitted.substring(beginIndex, endIndex);
				if (!originalAnnText.equals(splittedAnnText)) {
					log.warn("Splitted file %s does not match for position %s-%s:\n\torig: %s\n\tsplitted: %s",
							doc.getDocumentName(), beginIndex, endIndex, originalAnnText, splittedAnnText);
					isConsistent = false;
				}
			}
		}
		return isConsistent;
	}

}
