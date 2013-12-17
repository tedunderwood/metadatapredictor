package classification;
import java.util.ArrayList;

/**
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-16
 * Organizes all the Volumes being used for training and/or
 * classification.
 */
public class Collection {
	/**
	 * @param volumes List of all volumes in the collection.
	 * @param classMap Defines the classes for this classification task.
	 */
	ArrayList<Volume> volumes;
	ClassMap classMap;
}
