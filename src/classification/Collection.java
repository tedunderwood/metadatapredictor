package classification;
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
public class Collection {
	ArrayList<Volume> volumes;
	ArrayList<Double> weights;
	
	public Collection() {
		volumes = new ArrayList<Volume>();
		weights = new ArrayList<Double>();
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
	
}
