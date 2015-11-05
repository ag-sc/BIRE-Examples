package corpus.parser.usage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import corpus.AnnotatedDocument;
import corpus.DatasetConfig;
import corpus.DefaultCorpus;
import logging.Log;
import variables.State;

public class UsageLoader {

	public static void main(String[] args) {
		convertDatasetToJavaBinaries(DatasetConfig.getUSAGEJavaBinFilepath());
	}

	public static DefaultCorpus<AnnotatedDocument<State, State>> loadDatasetFromBinaries(String srcFilepath)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(srcFilepath));
		DefaultCorpus<AnnotatedDocument<State, State>> corpus = (DefaultCorpus) in.readObject();
		in.close();
		return corpus;
	}

	public static DefaultCorpus<AnnotatedDocument<State, State>> convertDatasetToJavaBinaries(String destFilepath) {
		File annDir = new File("/homes/sjebbara/datasets/USAGE-corpus-with-text/files/de");
		DefaultCorpus<AnnotatedDocument<State, State>> corpus = UsageParser.parseCorpus(annDir);
		try {
			System.out.println("store");
			saveCorpusToFile(corpus, destFilepath);
			Log.d("Corpus (%s documents) successfully parsed and stored to file \"%s\"", corpus.getDocuments().size(),
					destFilepath);
			System.out.println("done!");
			return corpus;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void saveCorpusToFile(DefaultCorpus<AnnotatedDocument<State, State>> corpus, String destFilepath)
			throws FileNotFoundException, IOException {
		File destFile = new File(destFilepath);
		File destDir = destFile.getParentFile();

		if (!destDir.exists() && !destDir.mkdirs()) {
			throw new IllegalStateException("Couldn't create dir: " + destDir);
		}
		ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(destFile));
		os.writeObject(corpus);
		os.close();
	}
}
