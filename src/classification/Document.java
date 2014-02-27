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
