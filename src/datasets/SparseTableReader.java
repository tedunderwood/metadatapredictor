package datasets;

import java.util.HashMap;
import java.util.Map;
import java.io.File;

import classification.LineReader;

public class SparseTableReader {
	String dataPath;
	static final int NUMCOLUMNS = 3;
	
	public SparseTableReader(String dataPath) {
		this.dataPath = dataPath;
	}
	
	/**
	 * Reads sparse tables formatted as a tsv where filename is the first column,
	 * word the second column, and count the third. We assume that we've been
	 * passed the path to a folder containing multiple such tables. We return the
	 * sparse table as a map where volumeID is the key to submaps, keyed by
	 * word.
	 * 
	 * @return A list of summary objects.
	 * @throws InputFileException
	 */
	public Map<String, HashMap<String, Integer>> turnTSVintoSummaries() throws InputFileException {
		// Get all the files in the folder.
		File folder = new File(dataPath);
		File[] listOfFiles = folder.listFiles();
	    
		// Initialize the map of maps.
		Map<String, HashMap<String, Integer>> collectedVolumes = new HashMap<String, HashMap<String, Integer>>();
		
		for (File nextFile : listOfFiles) {
			// check to make sure this is actually a data file and not e.g. a hidden file
			String thisFilename = nextFile.getName();
			if (!thisFilename.endsWith(".txt") & !thisFilename.endsWith(".tsv")) continue;
			
			LineReader textSource = new LineReader(nextFile);
			
			try {
				String[] filelines = textSource.readlines();
			
				
				for (String line : filelines) {
					String[] tokens = line.split("\t");
	
					int numFields = tokens.length;
					if (numFields != NUMCOLUMNS) {
						InputFileException cause = new InputFileException("Mismatch between number of fields and number of columns at" +
								" line\n" + line);
						throw cause;
					}
					
					String htid = tokens[0];
					String word = tokens[1];
					int wordcount = Integer.parseInt(tokens[2]);
					// We assume that the volume ID is in the first column of the table,
					// the word in the second, and wordcount in the third.
					
					// Try to get the existing map for this volume.
					Map<String, Integer> currentMap = collectedVolumes.get(htid);
					
					// If there is no map for it, make one.
					if (currentMap == null) {
						HashMap<String, Integer> newMap = new HashMap<String, Integer>();
						newMap.put(word, wordcount);
						collectedVolumes.put(htid, newMap);
					}
					else {
						currentMap.put(word, wordcount);
					}
				}
			}
			catch (InputFileException cause) {
				throw cause;
			}
		} // end for loop iterating across files.
		
		return collectedVolumes;
	}
}
