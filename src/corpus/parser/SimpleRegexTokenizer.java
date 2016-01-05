package corpus.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import corpus.Token;

public class SimpleRegexTokenizer implements Tokenizer {

	private Pattern pattern = Pattern.compile("[a-zA-Z]+|\\d+|[^\\w\\s]");

	@Override
	public List<Tokenization> tokenize(List<String> sentences) {
		List<Tokenization> tokenizations = new ArrayList<>();
		int accumulatedSentenceLength = 0;
		for (String sentence : sentences) {
			int index = 0;
			Matcher matcher = pattern.matcher(sentence);
			List<Token> tokens = new ArrayList<>();
			while (matcher.find()) {
				String text = matcher.group();
				int from = matcher.start();
				int to = matcher.end();
				tokens.add(new Token(index, from, to, text));
				index++;
			}
			Tokenization tokenization = new Tokenization(tokens, sentence, accumulatedSentenceLength);
			tokenizations.add(tokenization);
			accumulatedSentenceLength += sentence.length();
			// System.out.println(tokenization.originalSentence);
			// System.out.println(
			//	tokenization.tokens.stream().reduce("", (s, t) -> s + " " + t.getText(), (s, tt) -> s + tt));
		}
		return tokenizations;
	}

}
