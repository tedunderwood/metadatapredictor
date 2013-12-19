package classification;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author tunder
 * @version 1.0
 * @since 2013-12-16
 * Maps concrete dates, received as Strings, into bins that
 * represent segments on the timeline. Those bins can then be
 * used as classes in the classification process.
 * 
 * Classes are stored in two ways. An ArrayList of Integers
 * stores the midpoints of date bins. But in addition, every
 * ClassMap has an UNKNOWN class, indicating bad or missing
 * data, or works that for some reason should not be included
 * in training models.
 * 
 */
public class DateClassMap extends ClassMap {

	Collection collection;
	final String UNKNOWN = "__unknown__";
	public ArrayList<Integer> classLabels;
	HashMap<String, ArrayList<Volume>> classMembers;
	String fieldToCheck;
	int numBins;
	int binRadius;
	WarningLogger logger;

	/**
	 * Constructor that generates "bins" of dates, allowed to
	 * overlap, representing each bin by its midpoint, and
	 * saving a <code>binRadius</code> to indicate the number
	 * of years on either side to include. Note that it's possible
	 * for a volume to belong to more than one class, since binRadius
	 * can exceed binSpacing / 2.
	 * 
	 * @param startBinDate This will be the midpoint of the first bin.
	 * @param endBinDate This need not be the last midpoint, but in any
	 * case no midpoint can be > than it.
	 * @param binRadius Number of years on either side to include in bins.
	 * @param binSpacing How far apart bin midpoints are from each other.
	 * @param collection The <code>Collection</code> this bin will reference.
	 * @param fieldToCheck The field in that collection storing date info.
	 */
	public DateClassMap(int startBinDate, int endBinDate, int binRadius,
			int binSpacing, Collection collection, String fieldToCheck,
			WarningLogger logger) {
		this.collection = collection;
		this.fieldToCheck = fieldToCheck;
		this.binRadius = binRadius;
		this.logger = logger;
		
		classLabels = new ArrayList<Integer>();
		for (int i = startBinDate; i <= endBinDate; i = i + binSpacing) {
			classLabels.add(i);
		}
		numBins = classLabels.size();
	}
	
	/**
	 * Method with no parameters that simply tells the ClassMap
	 * to initialize a map of ClassMembers and populate it with
	 * <code>Volumes</code> matching the criteria for each class. Note that
	 * volumes can belong to more than one class.
	 * 
	 */
	public void mapVolsByMetadata() {
		// First we initialize the map
		classMembers = new HashMap<String, ArrayList<Volume>>();
		for (int midpoint : classLabels) {
			String label = Integer.toString(midpoint);
			ArrayList<Volume> emptyList = new ArrayList<Volume>();
			classMembers.put(label, emptyList);
		}
		
		ArrayList<Volume> volumes = collection.getVolumes();
		
		for (Volume vol: volumes) {
			String value = vol.getValue(fieldToCheck);
			if (value == null) {
				logger.logWarning("Field " + fieldToCheck + " not found in " + vol.htid);
			}
			// Maybe I should actually throw an Exception here.
			
			else {
				if (!isInteger(value)) {
					classMembers.get(UNKNOWN).add(vol);
				}
				// Dates that don't convert to integers put their volume in
				// the UNKNOWN class.
				
				else {
					int thisdate = Integer.parseInt(value);
					// Convert to integer so you can check < >.
					
					for (int midpoint : classLabels) {
						if (thisdate >= (midpoint - binRadius) & 
								thisdate <= (midpoint + binRadius)) {
							classMembers.get(Integer.toString(midpoint)).add(vol);
						}
						// Note that it's entirely possible for a Volume to be a
						// member of more than one class, since binRadius can exceed
						// binSpacing / 2.
					}
					
				}
			}
		}
	}
	
	public ArrayList<String> getValidClasses() {
		ArrayList<String> validClasses = new ArrayList<String>();
		for (int midpoint : classLabels) {
			validClasses.add(Integer.toString(midpoint));
		}
		return validClasses;
	}
	
	public ArrayList<Volume> getMembers(String aClass) {
		ArrayList<Volume> members = classMembers.get(aClass);
		if (members == null) {
			logger.logWarning("Class " + aClass + " was not found.");
		}
		return members;
	}
	
	/**
	 * Very simple static method to test whether a string can be converted
	 * to an Integer. Borrowed from corsiKlause on StackOverflow.
	 * @param s
	 * @return boolean value
	 */
	public static boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}

}
