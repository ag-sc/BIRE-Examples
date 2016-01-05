package corpus.parser;

import java.util.List;

public interface Tokenizer {
	public List<Tokenization> tokenize(List<String> sentences);
}
