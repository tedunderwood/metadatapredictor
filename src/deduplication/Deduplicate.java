package deduplication;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import classification.LineReader;
import classification.LineWriter;
import classification.WarningLogger;
import datasets.InputFileException;
import datasets.TaubMetadataReader;
import datasets.Collection;
import datasets.SparseTableReader;

public class Deduplicate {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WarningLogger.initializeLogger(true, "/Users/tunderwood/JavaWorkspace/errorlog.txt");
		// This sets the logger to write non-fatal exceptions to file.
		// A setting of (false, "") would set the logger to write them to
		// console.
		
		String metadataSource = "";
		String featureSource = "";
		String dataSource = "";
		String outputPath = "";
		
		String[] features;
		LineReader featureReader = new LineReader(featureSource);
		try {
			features = featureReader.readlines();
		}
		catch (InputFileException e) {
			String stacktrace = stacktraceToString(e);
			System.out.println("Exception in featureReader\n" + stacktrace);
			features = new String[0];
			System.exit(0);
		}
		
		TaubMetadataReader metadataReader = new TaubMetadataReader(metadataSource);
		String[] fields = {"recordid", "author", "title", "date", "totalwords"};
		Collection metadata;
		
		try {
			metadata = metadataReader.readTSV(fields);
		}
		catch (InputFileException e) {
			String stacktrace = stacktraceToString(e);
			System.out.println("Exception in metadataReader\n" + stacktrace);
			metadata = new Collection();
			System.exit(0);
		}
		
		SparseTableReader dataReader = new SparseTableReader(dataSource);
		Map<String, HashMap<String, Integer>> wordcounts = new HashMap<String, HashMap<String, Integer>>();
		try {
			 wordcounts = dataReader.readTSVasMap();
		}
		catch (InputFileException e) {
			String stacktrace = stacktraceToString(e);
			System.out.println("Exception in dataReader\n" + stacktrace);
			System.exit(0);
		}
		
		RecAndVolCorpus corpus = new RecAndVolCorpus(metadata, features, wordcounts);
		corpus.deduplicateCorpus();
		// That's where the actual work of detecting connections takes place.
		
		ArrayList<Connection> connections = corpus.getSortedConnections();
		int numberOfConnections = connections.size();
		String[] outputLines = new String[numberOfConnections];
		LineWriter output = new LineWriter(outputPath, false);
		output.send(outputLines);
	}
	
	private static String stacktraceToString(InputFileException e) {
	    return Arrays.toString(e.getStackTrace());
	}

}
