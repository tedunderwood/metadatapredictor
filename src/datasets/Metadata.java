package datasets;
import java.util.ArrayList;


/**
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-16
 * Organizes all the Volumes being used for training and/or
 * classification. Since a <code>Collection</code> can be mapped
 * by more than one <code>ClassMap</code>, it does not have a field
 * for its ClassMap.
 * 
 * @param volumes List of all volumes in the collection.
 * @param weights The weight for each volume. By default it's 1, but
 * it can be set to other values. This allows us to indicate that some
 * volumes should be used in training more often than others, or that
 * some volumes should not be used at all (weight zero).
 */
/**
 * @author tunder
 *
 */
public class Metadata {
	ArrayList<Volume> volumes;
	ArrayList<Double> weights;
	String[] fields;
	
	public Metadata(String[] fields) {
		volumes = new ArrayList<Volume>();
		weights = new ArrayList<Double>();
		this.fields = fields;
	}
	
	/**
	 * @param volume The Volume to be added.
	 * By default we give new volumes a weight of 1.
	 */
	public void addVolume(Volume volume) {
		volumes.add(volume);
		weights.add(1.0);
	}
	
	public ArrayList<Volume> getVolumes(){
		return volumes;
	}
	
	/**
	 * @param volsToGet An ArrayList of Volumes.
	 * @return matchingWeights An ArrayList of weights corresponding to the
	 * Volumes that were sent, in the same order.
	 */
	public ArrayList<Double> getWeights(ArrayList<Volume> volsToGet) {
		ArrayList<Double> matchingWeights = new ArrayList<Double>();
		for (Volume thisvol : volsToGet) {
			int index = volumes.indexOf(thisvol);
			if (index < 0) {
				RuntimeException problem = new ArrayStoreException("No matching volume in" +
						" Collection.getWeights");
				throw problem;
			}
			else {
				matchingWeights.add(weights.get(index));
			}
		}
		return matchingWeights;
	}
	
	public int getSize() {
		return volumes.size();
	}
	
	
	/**
	 * Should only be applied to collections that contain a "date" field. Returns
	 * an error value (negative) otherwise.
	 * 
	 * @return int date
	 */
	public int getMinDate(boolean tolerateParseErrors) {
		int errorDate;
		if (tolerateParseErrors) errorDate = 8000;
		else errorDate = -2;
		// If we tolerate parsing errors, we treat them as a large number unlikely
		// to become minDate. Otherwise, any parsing error in the collection will
		// cause the whole collection to return a negative (error) value.
		
		int minDate = 10000;
		// This code will obvs need deprecation in 10,000 AD.
		
		boolean containsDate = false;
		for (String field: fields) {
			if (field.equals("date")) containsDate = true;
		}
		if (containsDate) {
			for (Volume thisvolume: volumes) {
				int thisDate;
				String thisDateString = thisvolume.getValue("date");
				try {
					thisDate = Integer.parseInt(thisDateString);
				}
				catch (Exception e) {
					thisDate = errorDate;
					// errorDate varies, depending on whether we're told
					// to tolerate parsing errors.
				}
				if (thisDate < minDate) minDate = thisDate;
			}
		}
		else minDate = -1;
		// if this collection has no date field, return error
		
		return minDate;
	}
	
	/**
	 * Should only be applied to collections that contain a "date" field. Returns
	 * an error value (negative) otherwise.
	 * 
	 * @return int date
	 */
	public int getMaxDate(boolean tolerateParseErrors) {
		int errorDate;
		if (tolerateParseErrors) errorDate = 0;
		else errorDate = -2;
		// If we tolerate parsing errors, we treat them as a small number unlikely
		// to become maxDate. Otherwise, any parsing error in the collection will
		// cause the whole collection to return a negative (error) value.
		
		int maxDate = 0;
		
		boolean containsDate = false;
		for (String field: fields) {
			if (field.equals("date")) containsDate = true;
		}
		if (containsDate) {
			for (Volume thisvolume: volumes) {
				int thisDate;
				String thisDateString = thisvolume.getValue("date");
				try {
					thisDate = Integer.parseInt(thisDateString);
				}
				catch (Exception e) {
					thisDate = errorDate;
					// errorDate varies, depending on whether we're told
					// to tolerate parsing errors.
				}
				if (thisDate > maxDate) maxDate = thisDate;
			}
		}
		else maxDate = -1;
		// if this collection has no date field, return error
		
		return maxDate;
	}

}
