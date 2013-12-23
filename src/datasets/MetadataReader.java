package datasets;

/**
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-17
 * 
 * Abstract class defining a template for readers appropriate to
 * data sources in a particular environment.
 * 
 */
public abstract class MetadataReader {
	protected String dataSource;
	
	
	/**
	 * @param dataSource A way of identifying the object this Reader
	 * will read. In some cases a path to a file.
	 */
	public MetadataReader(String dataSource){
		this.dataSource = dataSource;
	}
}
