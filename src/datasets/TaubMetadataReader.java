package datasets;
import java.util.HashMap;

import classification.LineReader;


/**
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-17
 * 
 * A metadata reader that reads tab-separated tables.
 *
 */
public class TaubMetadataReader extends MetadataReader {

	/**
	 * @param dataSource
	 * Constructor from superclass.
	 */
	public TaubMetadataReader(String dataSource) {
		super(dataSource);
	}

	/**
	 * @param fields The column names to be read.
	 * @return A Collection of Volumes identified by volume ID (which
	 * must be the first line in the table), and also holding the metadata
	 * fields specified in <code>fields</code>
	 * @throws NoSuchFieldException
	 */
	public Collection readTSV(String[] fields) throws InputFileException {
		Collection collection = new Collection();
		LineReader textSource = new LineReader(dataSource);
		try {
			String[] filelines = textSource.readlines();
		
			boolean header = true;
			int numColumns = 0;
			int[] columnsToRead = new int[fields.length];
			
			for (String line : filelines) {
				String[] tokens = line.split("\t");
				if (header) {
					// This block executes for the first line you read.
					numColumns = tokens.length;
					int counter = 0;
					// We expect to find all the strings specified in 'fields' somewhere
					// in the first line of the table.
					for (int i = 0; i < fields.length; ++ i) {
						String thisfield = fields[i];
						for (int j = 0; j < numColumns; ++j) {
							if (tokens[j] == thisfield) {
								columnsToRead[i] = j;
								counter += 1;
							}
						}
					}
					header = false;
					if (counter < fields.length) {
						// We did not find all our fields in the columns.
						InputFileException cause = new InputFileException("TaubMetadataReader cannot find some fields" +
								" it is assigned in file header.");
						throw cause;
					}
				}
				else{
					// This code executes for all lines other than the first.
					int numFields = tokens.length;
					if (numFields != numColumns) {
						InputFileException cause = new InputFileException("Mismatch between number of fields and number of columns at" +
								" line\n" + line);
						throw cause;
					}
					
					String htid = tokens[0];
					// We assume that the volume ID is in the first column of the table.
					
					HashMap metadataValues = new HashMap<String, String>();
					for (int i = 0; i < numColumns; ++ i) {
						metadataValues.put(fields[i], tokens[columnsToRead[i]]);
						// The names of fields are in fields. columnsToRead indexes
						// the location of each field in the header line, thus it
						// can be used as an index for tokens.
					}
					Volume volume = new Volume(htid, metadataValues);
					collection.addVolume(volume);
				}
			}
		}
		catch (InputFileException cause) {
			throw cause;
		}
		
		return collection;
	}
}
