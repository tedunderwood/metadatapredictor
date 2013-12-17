/**
 * 
 */
package classification;

import java.util.HashMap;

/**
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-16
 * Fundamental data object holding metadata for a volume.
 * In many cases we're only going to be interested in a single
 * metadata field. I.e., in regularizing dates we're only
 * going to need to store dates; in regularizing language
 * we'll store language. But the class is written to allow
 * an indefinite number of fields so that it will be
 * extensible to cases that are more complex (for instance,
 * where we're interested in both date and genre).
 */
public class Volume {
	/**
	 * @param htid The HathiTrust vol ID for this volume.
	 * @param metadataValues Maps metadata field names to their contents. 
	 * All metadata fields have String contents here; translation of a
	 * string like "1842" to an integer will take place in the ClassMap.
	 * @param metadataPredictions Maps metadata field names to Prediction
	 * objects that hold the probability of different values. Fields
	 * in the Values map need not necessarily be in the Predictions map.
	 */
	String htid;
	HashMap<String, String> metadataValues;
	HashMap<String, Prediction> metadataPredictions;
	int numPages;
	int numWords;
	
	public Volume(String htid, HashMap<String, String> Values) {
		this.htid = htid;
		this.metadataValues = Values;
	}
}
