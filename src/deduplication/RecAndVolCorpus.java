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
import datasets.Metadata;

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
	Map<Integer, ArrayList<Summary>> blocks;
	Map<String, Double> averageFeatureFreqs;
	ArrayList<String> featureSequence;
	int numFeatures;
	
	static final int WINDOW = 4000;
	static final double FIRSTTHRESHOLD = 0.95;
	static final double HARDTHRESHOLD = 0.90;
	
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
	public RecAndVolCorpus(Metadata collection, String[] features, Map<String, HashMap<String, Integer>> wordcounts) {
		// The error-logging system implemented here allows for the possibility that we may not
		// have wordcount data for all the volumes in the collection. Mismatches get sent to
		// a log file, as long as they are below the ALLOWEDERRORS constant set above. (We don't
		// want to keep going if things are deeply dysfunctional.)
		int numberOfErrors = 0;
		// This counter keeps track of the number of errors so far.
		
		featureSequence = new ArrayList<String>();
		averageFeatureFreqs = new HashMap<String, Double>();
		// We're going to average feature frequencies in the first 200 volumes to
		// create a normalizing denominator.
		
		for (String feature : features) {
			featureSequence.add(feature);
			averageFeatureFreqs.put(feature, 1d);
			// 1 because Laplacian correction.
		}
		numFeatures = featureSequence.size();
		
		summaries = new ArrayList<Summary>();
		int counter = 0;
		
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
				counter += 1;
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
						
						if (counter < 200) {
							double currentCount = averageFeatureFreqs.get(featureSequence.get(i));
							currentCount += vector[i];
							averageFeatureFreqs.put(featureSequence.get(i), currentCount);
						}
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
		
		// In spite of its name, averageFeatureFreqs currently contains
		// sums rather than averages. It needs division to rectify this.
		for (String key : averageFeatureFreqs.keySet()) {
			double averageValue = averageFeatureFreqs.get(key) / 200d;
			averageFeatureFreqs.put(key, averageValue);
			System.out.println(key + " " + Double.toString(averageValue));
		}
	}
	
	public void normalizeSummaries() {
		for (Summary thisSum : summaries) {
			double[] vector = new double[numFeatures];
			for (int i = 0; i < numFeatures; ++i) {
				vector[i] = thisSum.rawfeatures[i];
			}
			
			for (int i = 0; i < numFeatures; ++i) {
				vector[i] = vector[i] / averageFeatureFreqs.get(featureSequence.get(i));
			}
			
			thisSum.setFeatures(vector);
			// for (Double value : vector) {
			// 	System.out.println(Double.toString(value));
			// } 
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
	
	/**
	 * This method divides the list of Summaries up by the number of words they contain,
	 * in order to accelerate the search for duplicate volumes. We divide the range of
	 * possible lengths into 4000-word blocks. To avoid possible edge effects, we add each volume 
	 * both to its own block and to those immediately above and below.
	 * 
	 */
	public void cacheWindows() {
		
		blocks = new HashMap<Integer, ArrayList<Summary>>();
		int skipped = 0;
		
		for (Summary doc : summaries) {
			if (doc.sumOfRawFeatures() < 1000) {
				skipped += 1;
				continue;
			}
			int blockIdx = doc.numWords / 4000;
			ArrayList<Summary> homeGroup = blocks.get(blockIdx);
			if (homeGroup == null) {
				ArrayList<Summary> newList = new ArrayList<Summary>();
				newList.add(doc);
				blocks.put(blockIdx, newList);
			}
			else {
				homeGroup.add(doc);
			}
			if (blockIdx > 0) {
				ArrayList<Summary> oneDown = blocks.get(blockIdx - 1);
				if (oneDown == null) {
					ArrayList<Summary> newList = new ArrayList<Summary>();
					newList.add(doc);
					blocks.put(blockIdx - 1, newList);
				}
				else {
					oneDown.add(doc);
				}
			}
			ArrayList<Summary> oneUp = blocks.get(blockIdx + 1);
			if (oneUp == null) {
				ArrayList<Summary> newList = new ArrayList<Summary>();
				newList.add(doc);
				blocks.put(blockIdx + 1, newList);
			}
			else {
				oneUp.add(doc);
			}
			
		}
		System.out.println("Skipped " + skipped + " documents as too small.");
	}
	
	/**
	 * Checks volume and record-level Summaries for substantial identity with each other,
	 * using cosine similarity. Uses cached blocks to accelerate iteration, thus "Faster."
	 * 
	 * If volumes have a reasonably close match according to cossim, it also checks Euclidean
	 * distance, in order to use that as further evidence.
	 * 
	 * @param limit Maximum number of volumes to check. If set to zero, check all.
	 */
	public void deduplicateFaster(int limit) {
		this.cacheWindows();
		connections = new ArrayList<Connection>();
		
		if (limit < 1 | limit > numDocuments) limit = numDocuments;
		// This only controls the outer loop.

		HashSet<Summary> alreadyChecked = new HashSet<Summary>();
		int counter = 0;
		
		for (int i = 0; i < limit; ++i) {
			Summary firstDocument = summaries.get(i);
			alreadyChecked.add(firstDocument);
			
			if (counter % 100 == 1) System.out.println(counter);
			counter += 1;
			
			if (firstDocument.sumOfRawFeatures() < 1000) continue;
			if (firstDocument.numWords < 10000) continue;
			
			int blockIdx = firstDocument.numWords / 4000;
			ArrayList<Summary> window = blocks.get(blockIdx);
			
			for (Summary secondDocument : window) {
				if (alreadyChecked.contains(secondDocument)) continue;
				int difference = firstDocument.getNumWords() - secondDocument.getNumWords();
				if (difference > -WINDOW & difference < WINDOW) {
					if (firstDocument.recordID.equals(secondDocument.recordID)) {
						continue;
					}
					
					Connection tentativeConnection = new Connection(firstDocument, secondDocument);
					double cossim = tentativeConnection.cossim;

					if (cossim > FIRSTTHRESHOLD) {
						double probability = tentativeConnection.calculateProbability();
						
						if (probability > HARDTHRESHOLD) {
							connections.add(tentativeConnection);
						}
					}
				
				} // end check whether this pair is in WINDOW
			} // end iterate across window
		} // end outer loop
	} // end method body
	
//	/**  DEPRECATED 12-29-2013
//	 * Checks volume and record-level Summaries for substantial identity with each other,
//	 * using cosine similarity.
//	 * @param limit Maximum number of volumes to check. If set to zero, check all.
//	 */
//	public void deduplicateCorpus(int limit) {
//		connections = new ArrayList<Connection>();
//		if (limit < 1 | limit > numDocuments) limit = numDocuments;
//		// This only controls the outer loop. The inner loop covers the whole corpus,
//		// even if the outer loop doesn't/
//		
//		int startInnerLoop = 0;
//		// The inner loop has a different size each time, to create a triangular
//		// matrix. If x has been compared to y we don't need to compare
//		// y to x. So when the outer loop has completed a row,
//		// we take it out of consideration as a column.
//		
//		for (int i = 0; i < limit; ++ i) {
//			Summary firstDocument = summaries.get(i);
//			startInnerLoop += 1;
//			if (startInnerLoop % 100 == 1) System.out.println(startInnerLoop);
//			if (firstDocument.numWords < 10000) continue;
//			
//			
//			for (int j = startInnerLoop; j < numDocuments; ++j) {
//				Summary secondDocument = summaries.get(j);
//				int difference = firstDocument.getNumWords() - secondDocument.getNumWords();
//				if (difference > -WINDOW & difference < WINDOW) {
//					if (firstDocument.recordID.equals(secondDocument.recordID)) {
//						continue;
//					}
//					double cossim = cosineSimilarity(firstDocument.getFeatures(), 
//							secondDocument.getFeatures());
//					if (cossim > THRESHOLD) {
//						double titlematch = JaccardSimilarity.jaccardSimilarity(firstDocument.normalizedTitle(), secondDocument.normalizedTitle());
//						double authormatch = JaccardSimilarity.jaccardSimilarity(firstDocument.normalizedAuthor(), secondDocument.normalizedAuthor());
//						Connection thisConnection = new Connection(firstDocument, secondDocument, cossim, 
//								titlematch, authormatch);
//						connections.add(thisConnection);
//					}
//				
//				}
//			}
//		}
//	}
	
	public ArrayList<Connection> getSortedConnections() {
		Collections.sort(connections);
		return connections;
	}
	
//	public static double probSummariesIdentical(Summary firstDocument, Summary secondDocument) {
//		double cossim = cosineSimilarity(firstDocument.getFeatures(), secondDocument.getFeatures());
//		// double titlematch = JaccardSimilarity.jaccardSimilarity(firstDocument.normalizedTitle(), secondDocument.normalizedTitle());
//		// double authormatch = JaccardSimilarity.jaccardSimilarity(firstDocument.normalizedAuthor(), secondDocument.normalizedAuthor());
//		// This applies coefficients learned through logistic regression.
//		// I no longer use title or author, because they caused spurious connections.
//		double exponent = (56.589 * cossim) - 55.559;
//		return 1 / (1 + Math.exp(-exponent));
//		// that's the logit function	
//	}
	
//	private static double cosineSimilarity(double[] first, double[] second) {
//		int vectorLength = first.length;
//		assert(first.length == second.length);
//		double dotProduct = 0d;
//		double firstMagnitude = 0d;
//		double secondMagnitude = 0d;
//		for (int i = 0; i < vectorLength; ++i){
//			dotProduct += first[i] * second[i];
//			firstMagnitude += first[i] * first[i];
//			secondMagnitude += second[i] * second[i];
//		}
//		firstMagnitude = Math.sqrt(firstMagnitude);
//		secondMagnitude = Math.sqrt(secondMagnitude);
//		double denominator = (firstMagnitude * secondMagnitude);
//		if (denominator < 0.1) {
//			return 0d;
//			// The logic here is twofold. A) We want to avoid division by zero.
//			// More importantly B) We want to ignore very short documents, or
//			// documents lacking English words.
//		}
//		else {
//			return dotProduct / denominator;
//		}
//	}
	
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



