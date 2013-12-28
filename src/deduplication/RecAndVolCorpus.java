package deduplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

import classification.Document;
import classification.VolumeReader;
import classification.WarningLogger;

import datasets.InputFileException;
import datasets.Volume;
import datasets.Collection;

/**
 * A data structure that collects volumes as well as the records
 * that encapsulate them, and stores them all as a single flat
 * sequence of summary datapoints, so that records can be compared 
 * to individual volumes for purposes of deduplication.
 * 
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-23
 * 
 * @param volumes A list of volumes included in the corpus.
 * @param summaries A list of all the summary datapoints included in the corpus.
 * @param WINDOW Constant indicating the radius of the doclength window to search
 * for possible matches.
 * @param THRESHOLD Constant marking the cosine-similarity cutoff below which we
 * ignore matches.
 */
public class RecAndVolCorpus {
	
	ArrayList<Volume> volumes;
	ArrayList<Summary> summaries;
	public int numDocuments;
	ArrayList<Connection> connections;
	static final int WINDOW = 5000;
	static final double THRESHOLD = 0.994;
	
	static final int ALLOWEDERRORS = 2500;
	
	/**
	 * Generates a corpus of Summary objects at both the Record and Volume
	 * levels to permit deduplication. Assumes that the wordcount data has already
	 * been read from file and stored in a map where volIDs act as keys to lower
	 * level maps keyed by word.
	 *  
	 * @param collection The collection storing metadata; it provides a list of
	 * volume IDs.
	 * @param features The array of features we're using for the deduplication process. 
	 * @param wordcounts The map of maps storing wordcounts.
	 */
	public RecAndVolCorpus(Collection collection, String[] features, Map<String, HashMap<String, Integer>> wordcounts) {
		// The error-logging system implemented here allows for the possibility that we may not
		// have wordcount data for all the volumes in the collection. Mismatches get sent to
		// a log file, as long as they are below the ALLOWEDERRORS constant set above. (We don't
		// want to keep going if things are deeply dysfunctional.)
		int numberOfErrors = 0;
		// This counter keeps track of the number of errors so far.
		
		ArrayList<String> featureSequence = new ArrayList<String>();
		for (String feature : features) {
			featureSequence.add(feature);
		}
		int numFeatures = featureSequence.size();
		summaries = new ArrayList<Summary>();
		
		ArrayList<Volume> volumes = collection.getVolumes();
		// The basic strategy here is to iterate over volumes in the collection,
		// find wordcount data for each, and turn the wordcounts into Summary objects that can be
		// efficiently deduplicated.
		
		for (Volume vol : volumes) {
			String volID = vol.htid;
			HashMap<String, Integer> volWordcounts = wordcounts.get(volID);
			if (volWordcounts == null) {
				WarningLogger.logWarning("Could not find\t" + vol.htid);
				numberOfErrors += 1;
				if (numberOfErrors > ALLOWEDERRORS) {
					throw new RuntimeException("Error allowance exceeded.");
				}
			}
			else {
				double[] vector = new double[numFeatures];
				// The VolumeReader is designed to produce Document objects for a generic classification
				// process. But in the Deduplication package we have a different task and are using
				// Summary objects -- the main difference being that feature values are stored simply
				// as an array rather than a HashMap. Optimization -- possibly premature optimization,
				// but there you have it.
				for (int i = 0; i < numFeatures; ++i) {
					try {
						Integer value = volWordcounts.get(featureSequence.get(i));
						if (value == null) vector[i] = 0d;
						else vector[i] = (double) value; 
					}
					catch (NullPointerException e) {
						System.out.println("Null pointer in retrieval from wordcount map!");
						System.out.println(i);
						System.out.println(featureSequence.get(i));
					}
				}
				
				Summary newSummary = new Summary(vol, vector);
				
				summaries.add(newSummary);
			}
		}
			
		System.out.println("Created " + Integer.toString(summaries.size()) + " vol-level Summary objects. Now for records.");
			
		makeRecordSummaries(numFeatures);

		numDocuments = summaries.size();
	}


	/**
	 * Generates a corpus of Summary objects at both the Record and Volume
	 * levels to permit deduplication. Assumes that the data input is coming
	 * from files stored in pairtree.
	 * 
	 * @param allVolumes Volumes to read for this corpus.
	 * @param dataPath Path to the root directory for volumes.
	 * @param featuresToLoad A list of features that we're going to load on a preliminary
	 * pass -- these are not necessarily the features that will be used ultimately for 
	 * classification.
	 */
	public RecAndVolCorpus(ArrayList<Volume> allVolumes, String dataPath, HashSet<String> featuresToLoad) {
	
		volumes = allVolumes;
		VolumeReader reader = new VolumeReader(dataPath);
		
		ArrayList<String> featureSequence = new ArrayList<String>();
		for (String feature : featuresToLoad) {
			featureSequence.add(feature);
		}
		int numFeatures = featureSequence.size();
		summaries = new ArrayList<Summary>();
		
		int numberOfErrors = 0;
		
		for (Volume vol : allVolumes) {
			try {
				Document newInstance = reader.getInstance(vol, featuresToLoad);
				// Attempt to load the data for this volume. If this fails, the instance will not be added
				// to any of the data structures in the corpus.
				// As we cycle through, we concatenate all volumes in a single list.
				
				double[] vector = new double[numFeatures];
				// The VolumeReader is designed to produce Document objects for a generic classification
				// process. But in the Deduplication package we have a different task and are using
				// Summary objects -- the main difference being that feature values are stored simply
				// as an array rather than a HashMap. Optimization -- possibly premature optimization,
				// but there you have it.
				for (int i = 0; i < numFeatures; ++i) {
					Double value = newInstance.getRawTermFreq(featureSequence.get(i));
					if (value == null) vector[i] = 0;
					else vector[i] = value; 
				}
				
				Summary newSummary = new Summary(vol, vector);
				
				summaries.add(newSummary);
			}
			catch (InputFileException e) {
				WarningLogger.logWarning("Could not find " + vol.htid);
				numberOfErrors += 1;
				if (numberOfErrors > ALLOWEDERRORS) {
					throw new RuntimeException("Error allowance exceeded.");
				}
			}
		}
		
		// We've added summaries for all the volume data points. Now we're going to create
		// record-level Summary objects.
		
		makeRecordSummaries(numFeatures);

		numDocuments = summaries.size();
	}

	/**
	 * Assumes that <code>summaries</code> already contains a list of volume-level
	 * Summary objects, and expands it by creating record-level objects for the
	 * records that encompass the volumes. This is necessary in deduplication,
	 * because it's quite possible that one volume is matched by a record that's
	 * a three-volume set.
	 */
	public void makeRecordSummaries(int numFeatures) {
		// We start by creating volume groups keyed by recordID in a map.
		HashMap<String, ArrayList<Summary>> sorter = new HashMap<String, ArrayList<Summary>>();
		for (Summary thisSummary : summaries) {
			String recordID = thisSummary.getRecordID();
			ArrayList<Summary> existing = sorter.get(recordID);
			if (existing == null) {
				ArrayList<Summary> newList = new ArrayList<Summary>();
				newList.add(thisSummary);
				sorter.put(recordID, newList);
			}
			else {
				existing.add(thisSummary);
			}
		}
		
		for (String key : sorter.keySet()) {
			ArrayList<Summary> thisList = sorter.get(key);
			if (thisList.size() > 1) {
				// we don't create summaries for records that contain only
				// a single volume
				Summary recordSummary = new Summary(key, numFeatures, thisList);
				summaries.add(recordSummary);
			}
		}
	}
	
	/**
	 * Checks volume and record-level Summaries for substantial identity with each other,
	 * using cosine similarity.
	 * @param limit Maximum number of volumes to check. If set to zero, check all.
	 */
	public void deduplicateCorpus(int limit) {
		connections = new ArrayList<Connection>();
		if (limit < 1 | limit > numDocuments) limit = numDocuments;
		// This only controls the outer loop. The inner loop covers the whole corpus,
		// even if the outer loop doesn't/
		
		int startInnerLoop = 0;
		// The inner loop has a different size each time, to create a triangular
		// matrix. If x has been compared to y we don't need to compare
		// y to x. So when the outer loop has completed a row,
		// we take it out of consideration as a column.
		
		for (int i = 0; i < limit; ++ i) {
			Summary firstDocument = summaries.get(i);
			startInnerLoop += 1;
			if (startInnerLoop % 100 == 1) System.out.println(startInnerLoop);
			if (firstDocument.numWords < 10000) continue;
			
			
			for (int j = startInnerLoop; j < numDocuments; ++j) {
				Summary secondDocument = summaries.get(j);
				int difference = firstDocument.getNumWords() - secondDocument.getNumWords();
				if (difference > -WINDOW & difference < WINDOW) {
					if (firstDocument.recordID.equals(secondDocument.recordID)) {
						continue;
					}
					double cossim = cosineSimilarity(firstDocument.getFeatures(), 
							secondDocument.getFeatures());
					if (cossim > THRESHOLD) {
						double titlematch = JaccardSimilarity.jaccardSimilarity(firstDocument.normalizedTitle(), secondDocument.normalizedTitle());
						double authormatch = JaccardSimilarity.jaccardSimilarity(firstDocument.normalizedAuthor(), secondDocument.normalizedAuthor());
						Connection thisConnection = new Connection(firstDocument, secondDocument, cossim, 
								titlematch, authormatch);
						connections.add(thisConnection);
					}
				
				}
			}
		}
	}
	
	public ArrayList<Connection> getSortedConnections() {
		Collections.sort(connections);
		return connections;
	}
	
	public static double probSummariesIdentical(Summary firstDocument, Summary secondDocument) {
		double cossim = cosineSimilarity(firstDocument.getFeatures(), secondDocument.getFeatures());
		double titlematch = JaccardSimilarity.jaccardSimilarity(firstDocument.normalizedTitle(), secondDocument.normalizedTitle());
		double authormatch = JaccardSimilarity.jaccardSimilarity(firstDocument.normalizedAuthor(), secondDocument.normalizedAuthor());
		// This applies coefficients learned through logistic regression. -1897 is the intercept.
		double exponent = (1899 * cossim) + (7.5 * titlematch) + (0.7 * authormatch) - 1897;
		return 1 / (1 + Math.exp(-exponent));
		// that's the logit function	
	}
	
	private static double cosineSimilarity(double[] first, double[] second) {
		int vectorLength = first.length;
		assert(first.length == second.length);
		double dotProduct = 0d;
		double firstMagnitude = 0d;
		double secondMagnitude = 0d;
		for (int i = 0; i < vectorLength; ++i){
			dotProduct += first[i] * second[i];
			firstMagnitude += Math.pow(first[i], 2);
			secondMagnitude += Math.pow(second[i], 2);
		}
		firstMagnitude = Math.sqrt(firstMagnitude);
		secondMagnitude = Math.sqrt(secondMagnitude);
		double denominator = (firstMagnitude * secondMagnitude);
		if (denominator < 1000) {
			return 0d;
			// The logic here is twofold. A) We want to avoid division by zero.
			// More importantly B) We want to ignore very short documents, or
			// documents lacking English words.
		}
		else {
			return dotProduct / denominator;
		}
	}
	
//	private double jaccardSimilarity(String first, String second) {
//		String[] firstset = first.toLowerCase().split("\\s+");
//		String[] secondset = second.toLowerCase().split("\\s+");
//		Set<String> s1 = new HashSet<String>(Arrays.asList(firstset));
//		Set<String> s2 = new HashSet<String>(Arrays.asList(secondset));
//		
//		Set<String> union = new HashSet<String>(s1);
//		union.addAll(s2);
//
//		Set<String> intersection = new HashSet<String>(s1);
//		intersection.retainAll(s2);
//		
//		if (union.size() == 0) {
//			return 0;
//		}
//		else {
//			return intersection.size() / union.size();
//		}
//	}
		
}



