package deduplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
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
	static final int WINDOW = 6000;
	static final double THRESHOLD = 0.98;
	
	static final int ALLOWEDERRORS = 10;
	
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
				WarningLogger.logWarning("Could not find " + vol.htid);
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
					Double value = (double) volWordcounts.get(featureSequence.get(i));
					if (value == null) vector[i] = 0d;
					else vector[i] = value; 
				}
				
				Summary newSummary = new Summary(vol, vector);
				
				summaries.add(newSummary);
			}
			makeRecordSummaries(numFeatures);

			numDocuments = summaries.size();
		}
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
	
	public void deduplicateCorpus() {
		connections = new ArrayList<Connection>();
		int startInnerLoop = 0;
		// The logic of these loops is that we want to create a triangular
		// matrix. If x has been compared to y we don't need to compare
		// y to x. So when the outer loop has completed a row,
		// we take it out of consideration as a column.
		
		for (int i = 0; i < numDocuments; ++ i) {
			Summary firstDocument = summaries.get(i);
			startInnerLoop += 1;
			
			for (int j = startInnerLoop; j < numDocuments; ++j) {
				Summary secondDocument = summaries.get(j);
				int difference = firstDocument.getNumWords() - secondDocument.getNumWords();
				if (difference > -WINDOW & difference < WINDOW) {
					if (firstDocument.recordID == secondDocument.recordID) {
						continue;
					}
					double cossim = cosineSimilarity(firstDocument.getFeatures(), 
							secondDocument.getFeatures());
					if (cossim > THRESHOLD) {
						double titlematch = jaccardSimilarity(firstDocument.title, secondDocument.title);
						double authormatch = jaccardSimilarity(firstDocument.author, secondDocument.author);
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
				
	private double cosineSimilarity(double[] first, double[] second) {
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
		if (denominator == 0) {
			return 0d;
		}
		else {
			return dotProduct / denominator;
		}
	}
	
	private double jaccardSimilarity(String first, String second) {
		first = first.toLowerCase();
		second = second.toLowerCase();
		String[] firstset = first.split(" ");
		String[] secondset = second.split(" ");
		Set<String> s1 = new HashSet<String>(Arrays.asList(firstset));
		Set<String> s2 = new HashSet<String>(Arrays.asList(secondset));
		
		Set<String> union = new HashSet<String>(s1);
		union.addAll(s2);

		Set<String> intersection = new HashSet<String>(s1);
		intersection.retainAll(s2);
		
		if (union.size() == 0) {
			return 0;
		}
		else {
			return intersection.size() / union.size();
		}
	}
		
}



