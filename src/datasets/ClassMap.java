package datasets;
import java.util.ArrayList;



/**
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-16
 * Abstract class defining a template that will map
 * concrete metadata values to generalized classLabels defined
 * for the purpose of a specific classification task.
 *
 */
public abstract class ClassMap {
	Metadata collection;
	final String UNKNOWN = "__unknown__";
	
	
	/**
	 * All instantiations of ClassMap should be able to return
	 * an ArrayList of Volumes matching a classLabel.
	 * @param aClass
	 * @return An ArrayList of Volumes matching aClass.
	 */
	public abstract ArrayList<Volume> getMembers(String aClass);
	
	/**
	 * All instantiations of ClassMap should have a method that
	 * returns an ArrayList of valid classes (i.e., excluding
	 * the special UNKNOWN class.)
	 * @return An ArrayList containing the names of all valid
	 * classes in this map.
	 */
	public abstract ArrayList<String> getValidClasses();
	
	/**
	 * This method is used to find negative instances for a training corpus.
	 * @param aClass
	 * @param n
	 * @return A random selection of n volumes not in aClass.
	 */
	public abstract ArrayList<Volume> getSelectedNonmembers(String aClass, int n);
	
	public abstract void mapVolsByMetadata();
}
