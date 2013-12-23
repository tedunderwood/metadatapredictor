package classification;
import java.util.ArrayList;
import java.util.HashSet;

import datasets.InputFileException;
import datasets.Volume;

/**
 * A data structure that collects volume-level Instances and maps them to their degree
 * of membership in a class. Corpora can be created for training or classification.
 * <p>
 * 
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-21
 * 
 * @param volumes A list of volumes included in the corpus.
 * @param documents A list of all the instances included in the corpus.
 */
public class VolumeCorpus extends Corpus {
	String classLabel = "__blank__";
	ArrayList<Volume> volumes;
	HashSet<String> featuresToLoad;
	ArrayList<Document> documents;
	ArrayList<Double> classMembership;
	ArrayList<Document> positiveInstances;
	ArrayList<Document> negativeInstances;
	public int numDocuments;
	
	static final int ALLOWEDERRORS = 10;
	
	/**
	 * Generates a training corpus of volume-level Instances by creating one Instance per Volume.
	 * In this constructor, we assume a crisp binary separation between pos/neg instances, but
	 * that doesn't have to be assumed in all constructors.
	 * 
	 * @param positives Volumes that should be positive instances.
	 * @param negatives Volumes that should be negative instances.
	 * @param dataPath Path to the root directory for volumes.
	 * @param featuresToLoad A list of features that we're going to load on a preliminary
	 * pass -- these are not necessarily the features that will be used ultimately for 
	 * classification.
	 */
	public VolumeCorpus(ArrayList<Volume> positives, ArrayList<Volume> negatives, String dataPath, HashSet<String> featuresToLoad, String classLabel) {
	
		this.featuresToLoad = featuresToLoad;
		this.classLabel = classLabel;
		VolumeReader reader = new VolumeReader(dataPath);
		classMembership = new ArrayList<Double>();
		
		int numberOfErrors = 0;
		
		for (Volume vol : positives) {
			try {
				Document newInstance = reader.getInstance(vol, featuresToLoad);
				// Attempt to load the data for this volume. If this fails, the instance will not be added
				// to any of the data structures in the corpus.
				volumes.add(vol);
				// As we cycle through, we concatenate all volumes in a single list.
				newInstance.setClassProb(1d);
				// Set the probability that it belongs to the class of this training corpus.
				// Since we're reading positives, it's 1.
				
				documents.add(newInstance);
				positiveInstances.add(newInstance);
			}
			catch (InputFileException e) {
				WarningLogger.logWarning("Could not find " + vol.htid);
				numberOfErrors += 1;
				if (numberOfErrors > ALLOWEDERRORS) {
					throw new RuntimeException("Error allowance exceeded.");
				}
			}
		}
		
		for (Volume vol : negatives) {
			try {
				Document newInstance = reader.getInstance(vol, featuresToLoad);
				// Attempt to load the data for this volume. If this fails, the instance will not be added
				// to any of the data structures in the corpus.
				volumes.add(vol);
				// As we cycle through, we concatenate all volumes in a single list.
				newInstance.setClassProb(0d);
				// Set the probability that it belongs to the class of this training corpus.
				// Since we're reading negatives, it's 0.
				
				documents.add(newInstance);
				negativeInstances.add(newInstance);
			}
			catch (InputFileException e) {
				WarningLogger.logWarning("Could not find " + vol.htid);
				numberOfErrors += 1;
				if (numberOfErrors > ALLOWEDERRORS) {
					throw new RuntimeException("Error allowance exceeded.");
				}
			}
		}
		
		for (Document thisInstance : documents) {
			classMembership.add(thisInstance.getClassProb());
		}
		
		numDocuments = documents.size();
	}
	
	public ArrayList<Document> getPositives() {
		return positiveInstances;
	}
	
	public ArrayList<Document> getNegatives() {
		return negativeInstances;
	}
	
	/**
	 * @return A list of all instances in the corpus. Note that each instance contains within
	 * it the probability that it belongs to the class in question, so although these are not
	 * sorted as "positives" or "negatives," that can be extracted from the ArrayList.
	 */
	public ArrayList<Document> getAllInstances() {
		return documents;
	}
	
	public ArrayList<Double> getMembershipProbs() {
		return classMembership;
	}
	
	public ArrayList<Double> normalizedFreqOverDocs(String term) {
		ArrayList<Double> distribution = new ArrayList<Double>(numDocuments);
		for (int i = 0; i < numDocuments; ++i) {
			Document thisInstance = documents.get(i);
			distribution.add(thisInstance.getNormalizedTermFreq(term));
		}
		return distribution;
	}

	public ArrayList<Double> rawFreqOverDocs(String term) {
		ArrayList<Double> distribution = new ArrayList<Double>(numDocuments);
		for (int i = 0; i < numDocuments; ++i) {
			Document thisInstance = documents.get(i);
			distribution.add(thisInstance.getRawTermFreq(term));
		}
		return distribution;
	}
}
