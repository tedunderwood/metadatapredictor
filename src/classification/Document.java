package classification;
import java.util.HashMap;

import datasets.Volume;

/**
 * @author tunderwood
 *
 */
public class Document {
	double probBelongsToClass;
	Volume vol;
	int page = -1;
	HashMap<String, Double> features;
	int numPages;
	double numWords;
	public boolean fileNotFound;
	
	/**
	 * Creates a Document (wordcount object), while explicitly flagging whether this Document is based on
	 * actual file data. There are cases where you want to put document in a list of docs,
	 * even if you couldn't actually find any data for it. In this case the fileNotFound
	 * flag becomes important. You could infer this from numWords == 0, but that's a
	 * workaround that could also break.
	 * 
	 * @param features A HashMap of Strings pointing to double feature counts. Would be
	 * more efficient, but less flexible, to store integers.
	 * @param vol The Volume (metadata record) associated with this Document.
	 * @param fileFound boolean flag.
	 * 
	 */
	public Document(HashMap<String, Double> features, Volume vol, boolean fileFound) {
		this.vol = vol;
		this.features = features;
		numPages = vol.getNumPages();
		numWords = 0d;
		// we add up all the word frequencies to produce a total number of words
		// this is only meaningful if the values of the feature map are unnormalized counts
		// but then again, we're only going to need/use numWords for normalization
		// in that case
		if (fileFound) {
			for (Double value : features.values()) {
				numWords += value;		
			}
			fileNotFound = false;
		}
		else {
			fileNotFound = true;
		}
	}
	
	public Document(HashMap<String, Double> features, Volume vol) {
		this.vol = vol;
		this.features = features;
		numPages = vol.getNumPages();
		numWords = 0d;
		// we add up all the word frequencies to produce a total number of words
		// this is only meaningful if the values of the feature map are unnormalized counts
		// but then again, we're only going to need/use numWords for normalization
		// in that case
		for (Double value : features.values()) {
			numWords += value;		
		}
		fileNotFound = false;
		// This constructor simply assumes the data is based on successful file access.
		// Deprecated.
	}
	
	public void setClassProb(double probBelongsToClass) {
		this.probBelongsToClass = probBelongsToClass;
	}
	
	public double getClassProb(){
		return probBelongsToClass;
	}
	
	public double getRawTermFreq(String term) {
		return features.get(term);
	}
	
	public double getNumWords() {
		return numWords;
	}
	
	public double termNormalizedByWordcount(String term) {
		if (numWords > 0 & features.containsKey(term)) {
			return (features.get(term) / numWords);
		}
		else return 0;
	}
	
	/**
	 * This method is deprecated, but I'm leaving it here for now because I don't
	 * know what weird function it plays in the "deduplication" package. Why would you
	 * ever divide term freq by number of *pages*? Idk! It's used in VolumeCorpus.
	 * Generally, prefer termNormalizedByWordcount, above.
	 * @param term
	 * @return
	 */
	public double getNormalizedTermFreq(String term) {
		return (features.get(term) / numPages);
	}
	
	public HashMap<String, Double> getFeatures(){
		return features;
	}

}
