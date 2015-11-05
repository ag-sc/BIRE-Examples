package corpus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import corpus.AnnotatedDocument;
import corpus.Corpus;

public class DefaultCorpus<DocumentT extends AnnotatedDocument<?, ?>> implements Corpus<DocumentT> {

	private List<DocumentT> documents = new ArrayList<>();
	private AnnotationConfig corpusConfig;

	public DefaultCorpus(AnnotationConfig config) {
		this.corpusConfig = config;
	}

	public AnnotationConfig getCorpusConfig() {
		return corpusConfig;
	}

	@Override
	public List<DocumentT> getDocuments() {
		return documents;
	}

	@Override
	public void addDocument(DocumentT doc) {
		this.documents.add(doc);
	}

	@Override
	public void addDocuments(Collection<DocumentT> documents) {
		this.documents.addAll(documents);
	}

	@Override
	public String toString() {
		return "BratCorpus [corpusConfig=" + corpusConfig + ", #documents=" + documents.size() + "]";
	}

	public String toDetailedString() {
		StringBuilder builder = new StringBuilder();
		for (AnnotatedDocument<?, ?> doc : documents) {
			builder.append(doc.getName());
			builder.append("\n\t");
			builder.append(doc.getContent());
			builder.append("\n\t");
			builder.append(doc.getTokens());
			builder.append("\n\t");
			builder.append(doc.getGoldResult());
			builder.append("\n");
		}
		return "BratCorpus [corpusConfig=" + corpusConfig + ", #documents=" + documents.size() + ", documents=\n"
				+ builder.toString() + "]";
	}

}
