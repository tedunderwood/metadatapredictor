package classification;

import datasets.ClassMap;
import datasets.Collection;

/**
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-16
 * Abstract class defining a template for classification processes.
 * The subclasses that instantiate it guide a concrete classification
 * workflow for some specific task.
 */
public abstract class ClassificationProcess {
	/**
	 * @param metadataPath The path to the root folder for metadata.
	 * @param dataPath  The path to the root folder for data.
	 * @param collection The collection to be classified.
	 * @param classMap Contains a list of classLabels permitted
	 * in this classification task.
	 */
	private String metadataPath;
	private String dataPath;
	private Collection collection;
	private ClassMap classMap;
	

}
