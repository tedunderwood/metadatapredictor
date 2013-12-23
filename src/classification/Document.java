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
	
	public Document(HashMap<String, Double> features, Volume vol) {
		this.vol = vol;
		this.features = features;
		numPages = vol.getNumPages();
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
	
	public double getNormalizedTermFreq(String term) {
		return (features.get(term) / numPages);
	}

}
