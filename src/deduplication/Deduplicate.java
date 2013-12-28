package deduplication;

import java.util.*;

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
		WarningLogger.initializeLogger(true, "/Users/tunderwood/deduplication/errorlog.txt");
		// This sets the logger to write non-fatal exceptions to file.
		// A setting of (false, "") would set the logger to write them to
		// console.
		
		String metadataSource = "/Users/tunderwood/deduplication/metadata.tsv";
		String featureSource = "/Users/tunderwood/deduplication/40words.txt";
		String dataSource = "/Users/tunderwood/deduplication/sparsetables";
		String outputPath = "/Users/tunderwood/deduplication/connections.txt";
		String clusterPath = "/Users/tunderwood/deduplication/clusters.txt";
		
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
		System.out.println("Done reading features.");
		
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
		System.out.println("Done reading metadata.");
		
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
		System.out.println("Done loading data.");
		System.out.println("Loaded " + Integer.toString(wordcounts.size()) + " volume IDs as data.");
		
		RecAndVolCorpus corpus = new RecAndVolCorpus(metadata, features, wordcounts);
		System.out.println("Created corpus of volume and record-level objects to compare.");
		
		corpus.deduplicateCorpus(0);
		// That's where the actual work of detecting connections takes place.
		// Setting the limit to zero causes it to work on the whole collection.
		System.out.println("Deduplicated the corpus.");
		
		ArrayList<Connection> connections = corpus.getSortedConnections();
		// The connections are sorted in order of cosine similarity.
		
		int numberOfConnections = connections.size();
		String[] outputLines = new String[numberOfConnections];
		LineWriter output = new LineWriter(outputPath, false);
		// The "false" means it's not set to append if output file already exists.
		
		for (int i = 0; i < numberOfConnections; ++i) {
			outputLines[i] = connections.get(i).outputLine();
		}
		
		output.send(outputLines);
		
		HierarchicalClusters fusion = new HierarchicalClusters(corpus.summaries, corpus.connections);
		ArrayList<HashSet<Summary>> clusters = fusion.sortClustersBySize();
		
		LineWriter outputStream = new LineWriter(clusterPath, true);
		int counter = 0;
				
		for (HashSet<Summary> cluster : clusters) {
			if (cluster.size() > 1) {
				outputStream.print(Integer.toString(counter));
				outputCluster(outputStream, cluster);
			}
			counter += 1;
		}
	}
	
	private static String stacktraceToString(InputFileException e) {
	    return Arrays.toString(e.getStackTrace());
	}
	
	private static void outputCluster(LineWriter out, HashSet<Summary> cluster) {
		Iterator<Summary> iterateCluster = cluster.iterator();
		while (iterateCluster.hasNext()) {
			Summary next = iterateCluster.next();
			out.print(next.outputName());
		}
	}

}
