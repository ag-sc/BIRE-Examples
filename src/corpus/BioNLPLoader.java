package corpus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.parser.JavaSentenceSplitter;
import corpus.parser.SimpleRegexTokenizer;
import corpus.parser.Tokenization;
import corpus.parser.Tokenizer;
import corpus.parser.bionlp.BioNLP2BIREConverter;
import corpus.parser.bionlp.BratAnnotatedDocument;
import corpus.parser.bionlp.BratAnnotationParser;
import corpus.parser.bionlp.BratConfigReader;
import corpus.parser.bionlp.exceptions.AnnotationFileException;

public class BioNLPLoader {
	private static Logger log = LogManager.getFormatterLogger();

	public static final File TOKEN_MODEL_FILE = new File("res/bionlp/julie/models/JULIE_life-science-1.6-token.mod.gz");
	public static final File SENTENCE_MODEL_FILE = new File(
			"res/bionlp/julie/models/JULIE_life-science-1.6-sentence.mod.gz");

	// private static Tokenizer tokenizer = new StanfordTokenizer();
	private static Tokenizer tokenizer = new SimpleRegexTokenizer();

	public static void main(String[] args) {
		BioNLPCorpus train = loadBioNLP2013Train(true);
		int a1TTrain = BioNLP2BIREConverter.a1TextBoundMistmatchCounter;
		int a2TTrain = BioNLP2BIREConverter.a2TextBoundMistmatchCounter;
		int a2ETrain = BioNLP2BIREConverter.a2EventMistmatchCounter;
		int mdTrain = BioNLP2BIREConverter.missedDocumentCounter;
		BioNLP2BIREConverter.a1TextBoundMistmatchCounter = 0;
		BioNLP2BIREConverter.a2TextBoundMistmatchCounter = 0;
		BioNLP2BIREConverter.a2EventMistmatchCounter = 0;
		BioNLP2BIREConverter.missedDocumentCounter = 0;
		BioNLPCorpus dev = loadBioNLP2013Dev(true);
		int a1TDev = BioNLP2BIREConverter.a1TextBoundMistmatchCounter;
		int a2TDev = BioNLP2BIREConverter.a2TextBoundMistmatchCounter;
		int a2EDev = BioNLP2BIREConverter.a2EventMistmatchCounter;
		int mdDev = BioNLP2BIREConverter.missedDocumentCounter;

		log.info("##### BioNLP 2013 #####");
		log.info("Train: %s documents. (%s, %s, %s) missed annotations -> %s missed documents.",
				train.getDocuments().size(), a1TTrain, a2TTrain, a2ETrain, mdTrain);
		log.info("Dev: %s documents. (%s, %s, %s) missed annotations -> %s missed documents.",
				dev.getDocuments().size(), a1TDev, a2TDev, a2EDev, mdDev);
	}

	public static BioNLPCorpus setupCorpus(File configFile) {
		BratConfigReader confReader = new BratConfigReader();
		log.debug("### Annotation configuration:");
		AnnotationConfig config = confReader.readConfig(configFile);
		log.debug("%s", config);
		return new BioNLPCorpus(config);
	}

	public static BioNLPCorpus defaultCorpus() {
		return setupCorpus(new File(DatasetConfig.getBioNLP2013ConfigFilepath()));
	}

	public static BioNLPCorpus loadBioNLP2013Train(boolean forceParsing) {
		return loadBioNLP2013(DatasetConfig.getBioNLP2013TrainPath(), DatasetConfig.getBioNLP2013TrainJavaBinFilepath(),
				forceParsing);
	}

	public static BioNLPCorpus loadBioNLP2013Dev(boolean forceParsing) {
		return loadBioNLP2013(DatasetConfig.getBioNLP2013DevPath(), DatasetConfig.getBioNLP2013DevJavaBinFilepath(),
				forceParsing);
	}

	// public static Corpus loadBioNLP2013Test() {
	// return loadBioNLP2013(Constants.getBioNLP2013TestPath(),
	// Constants.getBioNLP2013TestJavaBinFilepath());
	// }

	private static BioNLPCorpus loadBioNLP2013(String dirpath, String serializationFilepath, boolean forceParsing) {
		if (!forceParsing) {
			try {
				return loadDatasetFromBinaries(serializationFilepath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		List<File> texts = FileUtils.getFiles(new File(dirpath), "txt");
		List<File> annotationsA1 = FileUtils.getFiles(new File(dirpath), "a1");
		List<File> annotationsA2 = FileUtils.getFiles(new File(dirpath), "a2");
		return convertDatasetToJavaBinaries(texts, annotationsA1, annotationsA2, serializationFilepath);
	}

	public static BioNLPCorpus loadDatasetFromBinaries(String srcFilepath)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(srcFilepath));
		BioNLPCorpus corpus = (BioNLPCorpus) in.readObject();
		in.close();
		return corpus;
	}

	/**
	 * This method parses the BioNLP documents that are given as parameters.
	 * 
	 * @param destFilepath
	 * @return
	 */
	public static BioNLPCorpus convertDatasetToJavaBinaries(List<File> texts, List<File> annotationsA1,
			List<File> annotationsA2, String destFilepath) {
		BioNLPCorpus corpus = defaultCorpus();

		Map<String, File> annotationA1Files = getMapOfFiles(annotationsA1);
		Map<String, File> annotationA2Files = getMapOfFiles(annotationsA2);
		Map<String, File> textFiles = getMapOfFiles(texts);

		// create a set of documents for which we have both the annotations file
		// and the raw text file.
		Set<String> completeDocuments = new HashSet<String>(textFiles.keySet());
		completeDocuments.retainAll(annotationA1Files.keySet());
		completeDocuments.retainAll(annotationA2Files.keySet());

		log.debug("%s documents with a given text and annotation files", completeDocuments.size());
		log.debug("filesnames: %s", completeDocuments);

		int current = 1;
		for (String filename : completeDocuments) {
			log.debug("#############################");
			log.debug("#############################");
			log.debug("parse document \"%s\" (%s/%s)", filename, current, completeDocuments.size());

			File annA1File = annotationA1Files.get(filename);
			File annA2File = annotationA2Files.get(filename);
			File textFile = textFiles.get(filename);
			try {
				loadDocuments(corpus, textFile, Arrays.asList(annA1File, annA2File));
			} catch (Exception e1) {
				e1.printStackTrace();
				log.warn("Parsing of files for %s not possible. Skip this instance", filename);
			}
			current++;
		}
		if (destFilepath != null) {
			try {
				System.out.println("store");
				saveCorpusToFile(corpus, destFilepath);
				log.debug("Corpus (%s documents) successfully parsed and stored to file \"%s\"",
						corpus.getDocuments().size(), destFilepath);
				System.out.println("done!");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return corpus;
	}

	/**
	 * Parses the provided files and adds the extracted documents to the corpus.
	 * Since each single sentence is considered as a document, this methods
	 * might add several documents to the corpus.
	 * 
	 * @param corpus
	 * @param textFile
	 * @param sentFile
	 * @param annFiles
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static void loadDocuments(BioNLPCorpus corpus, File textFile, List<File> annFiles)
			throws FileNotFoundException, ClassNotFoundException, IOException {

		log.debug("#####################");
		log.debug("### Brat annotations...");
		BratAnnotationParser parser = new BratAnnotationParser();
		BratAnnotatedDocument bratDoc = parser.parseFile(textFile, annFiles);

		log.debug("#####################");
		log.debug("### Text splitting in sentences...");
		List<String> sentences = JavaSentenceSplitter.getSentencesAsList(textFile);
		log.debug("#####################");
		log.debug("### Tokenization of sentences...");
		List<Tokenization> tokenizations = tokenizer.tokenize(sentences);

		log.debug("#####################");
		log.debug("### BIRE annotations...");
		List<SubDocument> documents;
		try {
			documents = BioNLP2BIREConverter.convert(bratDoc, tokenizations);
			corpus.addDocuments(documents);
		} catch (AnnotationFileException e) {
			e.printStackTrace();
		}
	}

	public static BioNLPCorpus loadDocument(File textFile, List<File> annFiles)
			throws FileNotFoundException, ClassNotFoundException, IOException {
		BioNLPCorpus corpus = defaultCorpus();
		loadDocuments(corpus, textFile, annFiles);
		return corpus;
	}

	private static void saveCorpusToFile(BioNLPCorpus corpus, String destFilepath)
			throws FileNotFoundException, IOException {
		File destFile = new File(destFilepath);
		FileUtils.makeParents(destFile);
		ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(destFile));
		os.writeObject(corpus);
		os.close();
	}

	private static Map<String, File> getMapOfFiles(List<File> annotationsA1) {
		Map<String, File> files = new HashMap<String, File>();
		for (File file : annotationsA1) {
			String fileBasename = FileUtils.getFilenameWithoutExtension(file.getName());
			files.put(fileBasename, file);
		}
		return files;
	}

}
