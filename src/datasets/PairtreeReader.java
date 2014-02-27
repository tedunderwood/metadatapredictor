package datasets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.io.File;

import classification.Document;
import classification.Pairtree;
import classification.WarningLogger;

import classification.LineReader;

public class PairtreeReader {
	String dataPath;
	static final int NUMCOLUMNS = 3;
	Pairtree pairtree;
	WarningLogger logger;
	
	public PairtreeReader(String dataPath) {
		this.dataPath = dataPath;
		this.pairtree = new Pairtree();
	}
	
	private String getPairtreePath(Volume vol) {
		String dirtyID = vol.htid;
		int periodIndex = dirtyID.indexOf(".");
		String prefix = dirtyID.substring(0, periodIndex);
		// the part before the period
		String pathPart = dirtyID.substring(periodIndex+1);
		// everything after the period
		String ppath = pairtree.mapToPPath(pathPart);
		String encapsulatingDirectory = pairtree.cleanId(pathPart);
		String wholePath = dataPath + prefix + "/pairtree_root/" + ppath + "/"+ encapsulatingDirectory + 
				"/" + encapsulatingDirectory + ".vol.tsv";
		return wholePath;
	}
	
	public Document getDocument(Volume vol, HashSet<String> featuresToLoad) {
		String path = getPairtreePath(vol);
		LineReader reader = new LineReader(path);
		boolean loadAll = false;
		// if we aren't given a feature list, load all features
		
		if (featuresToLoad.size() < 1) {
			loadAll = true;
		}
		
		HashMap<String, Double> wordcounts = new HashMap<String, Double>();
		boolean fileFound = false;
		
		try {
			String filelines[] = reader.readlines();

			for (String line : filelines){
				String[] tokens = line.split("\t");
				String word = tokens[0];
				if (featuresToLoad.contains(word) | loadAll) {
					Double count = Double.parseDouble(tokens[1]);
					wordcounts.put(word, count);
				}
			}
			fileFound = true;	
		}
		catch (InputFileException e) {
			WarningLogger.addFileNotFound(path);
			System.out.println("File not found: " + path);
			fileFound = false;
		}
		Document newInstance = new Document(wordcounts, vol, fileFound);
		return newInstance;
	}
	
	/**
	 * This method gets a list of Document objects using the pairtree paths implied by volume IDs in
	 * a list of Volume objects. It stores wordcounts in the Documents as a map of words -> double 
	 * values that can represent (normalized or raw) wordcounts. Default assumption is that they're
	 * raw and need to be normalized later. Note that this method *does not* guarantee that the 
	 * length of the list it returns will match the length of the list it's sent, because it 
	 * does not add Documents in cases where the matching wordcount data could not be found.
	 * When using this method, you should use the volume field in the Document object to establish
	 * identity. Don't assume a 1-to-1 mapping.
	 * 
	 * @param vols
	 * @param featuresToLoad
	 * @return
	 */
	public ArrayList<Document> getMultipleDocs(ArrayList<Volume> vols, HashSet<String> featuresToLoad) {
		ArrayList<Document> docList = new ArrayList<Document>();
		for (Volume vol : vols) {
			Document thisDoc = getDocument(vol, featuresToLoad);
			if (!thisDoc.fileNotFound) docList.add(thisDoc);
			// If the document isn't based on actual file data, we don't add it to the list.
		}
		return docList;
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
	public Map<String, HashMap<String, Integer>> readTSVasMap() throws InputFileException {
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