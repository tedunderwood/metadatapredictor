package classification;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import datasets.InputFileException;
import datasets.Volume;

public class VolumeReader {
	String dataPath;
	Pairtree pairtree;
	
	public VolumeReader(String dataPath) {
		this.dataPath = dataPath;
		pairtree = new Pairtree();
		
	}
	
	public Document getInstance(Volume vol, HashSet<String> featuresToLoad) throws InputFileException {
		String dirtyID = vol.htid;
		int periodIndex = dirtyID.indexOf(".");
		String prefix = dirtyID.substring(0, periodIndex);
		// the part before the period
		String pathPart = dirtyID.substring(periodIndex+1);
		// everything after the period
		String ppath = pairtree.mapToPPath(pathPart);
		String encapsulatingDirectory = pairtree.cleanId(pathPart);
		String wholePath = dataPath + "/" + prefix + "/pairtree_root/" + ppath + "/"+ encapsulatingDirectory + 
				"/" + encapsulatingDirectory + "vol.tsv";

		LineReader reader = new LineReader(wholePath);
		
		HashMap<String, Double> wordcounts = new HashMap<String, Double>();
		try {
			String filelines[] = reader.readlines();
			for (String line : filelines){
				String[] tokens = line.split("\t");
				String word = tokens[1];
				if (featuresToLoad.contains(word)) {
					Double count = Double.parseDouble(tokens[2]);
					wordcounts.put(word, count);
				}
			}
			
		}
		catch (InputFileException e) {
			WarningLogger.logWarning("Could not find " + wholePath);
			throw e;
		}
		Document newInstance = new Document(wordcounts, vol);
		return newInstance;
	}
}
