package corpus.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import corpus.Token;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class StanfordTokenizer implements Tokenizer {

	private CoreLabelTokenFactory factory;

	public StanfordTokenizer() {
		factory = new CoreLabelTokenFactory();
	}

	@Override
	public List<Tokenization> tokenize(List<String> sentences) {
		List<Tokenization> tokenizations = new ArrayList<>();
		int accumulatedSentenceLength = 0;
		for (String sentence : sentences) {
			int index = 0;
			PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new StringReader(sentence), factory, "");
			List<Token> tokens = new ArrayList<>();
			for (CoreLabel label : ptbt.tokenize()) {
				tokens.add(new Token(index, label.beginPosition(), label.endPosition(), label.originalText()));
				index++;
			}
			tokenizations.add(new Tokenization(tokens, sentence, accumulatedSentenceLength));
			accumulatedSentenceLength += sentence.length();
		}
		return tokenizations;
	}

}
