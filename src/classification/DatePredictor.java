package classification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import datasets.*;

public class DatePredictor {
	
	static Metadata metadata;
	static DateClassMap classMap;

	/**
	 * @author tunderwood
	 * @version 1.0
	 * @since 2014-2-23
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String metadataFile = args[0];
		String dataSource = args[1];
		String[] fieldList = {"date", "totalpages", "totalwords"};
		int binRadius = Integer.parseInt(args[2]);
		int vocabularySize = Integer.parseInt(args[3]);
		int maxVolsToRead = Integer.parseInt(args[4]);
		String ridgeParameter = args[5];
		
		MetadataReader metadataReader = new TaubMetadataReader(metadataFile);
		
		try{
			metadata = metadataReader.readTSV(fieldList);
		}
		catch (InputFileException e) {
			String stacktrace = stacktraceToString(e);
			System.out.println("Exception in metadataReader\n" + stacktrace);
			metadata = new Metadata(fieldList);
			System.exit(0);
		}
		System.out.println("Done reading metadata.");
		
		int numVolumes = metadata.getSize();
		int startDate = metadata.getMinDate(true);
		int endDate = metadata.getMaxDate(true);
		// both boolean flags are true because we tolerate errors in date parsing
		if (endDate < 0 | startDate < 0) {
			System.out.println("Error condition:");
			System.out.println("Start date = " + Integer.toString(startDate));
			System.out.println("End date = " + Integer.toString(endDate));
			System.exit(1);
		}
		int firstBinMidpoint = startDate + binRadius;
		int binSpacing = (binRadius * 2) + 1;
		// So, for instance, a startDate of 1700 with binRadius of 2 produces
		// firstBinMidpoint 1702 and
		// binSpacing 5
		
		classMap = new DateClassMap(firstBinMidpoint, endDate, binRadius,
			binSpacing, metadata, "date");
		classMap.mapVolsByMetadata();
		
		ArrayList<String> classLabels = classMap.getKnownClasses();
		int classCount = classLabels.size();
		
		int minClassSize = 10000000;
		for (String label : classLabels) {
			int thisSize = classMap.getClassSize(label);
			if (thisSize < minClassSize) minClassSize = thisSize;
		}
		
		PairtreeReader dataReader = new PairtreeReader(dataSource);
		ArrayList<String> orderedVocabulary = buildVocabulary(classLabels, vocabularySize, dataReader);
		HashSet<String> vocabulary = new HashSet<String>(orderedVocabulary);
		
		// Now we're going to build a model for each class in the classMap, and save it.
		int maxSetSize = maxVolsToRead / 2;
		int setSize;
		ArrayList<LogisticClassifier> models = new ArrayList<LogisticClassifier>(classCount);
		
		for (String label : classLabels) {
			int thisSize = classMap.getClassSize(label);
			if (thisSize > maxSetSize) setSize = maxSetSize;
			else setSize = thisSize;
			ArrayList<Volume> positiveVols = classMap.takeRandomSample(label, setSize);
			ArrayList<Volume> negativeVols = classMap.stratifiedSampleExcept(label, setSize);
			ArrayList<Document> positiveDocs = dataReader.getMultipleDocs(positiveVols, vocabulary);
			ArrayList<Document> negativeDocs = dataReader.getMultipleDocs(negativeVols, vocabulary);
			ArrayList<Document> allDocs = new ArrayList<Document>(positiveDocs);
			allDocs.addAll(negativeDocs);
			ArrayList<Double> classValues = new ArrayList<Double>();
			int sizeOfWholeSet = allDocs.size();
			int numPositives = positiveDocs.size();
			for (int i = 0; i < sizeOfWholeSet; ++ i) {
				if (i < numPositives) classValues.add(1d);
				else classValues.add(0d);
			}
			// We now have a list of positive and negative examples, and a list of
			// classValues indexed to it, containing 1 for positive examples and 0d for
			// negative ones.
			LogisticClassifier thisClassifier = new LogisticClassifier(label, orderedVocabulary, allDocs, classValues, ridgeParameter);
			models.add(thisClassifier);
		}
	}
	
	private static String stacktraceToString(InputFileException e) {
	    return Arrays.toString(e.getStackTrace());
	}
	
	private static ArrayList<String> buildVocabulary(ArrayList<String> classLabels, int vocabularySize, PairtreeReader dataReader) {
		// Let's build a vocabulary
		HashSet<String> featuresToLoad = new HashSet<String>();
		// We leave this empty, which ensures loading all features.
		HashMap<String, Integer> wordcounts = new HashMap<String, Integer>();
		
		int volumesPerClass = 100;
		for (String label : classLabels) {
			int volumesToGet;
			int thisSize = classMap.getClassSize(label);
			if (thisSize < volumesPerClass) volumesToGet = thisSize;
			else volumesToGet = volumesPerClass;
			ArrayList<Volume> selectedVols = classMap.takeRandomSample(label, volumesToGet);
			ArrayList<Document> selectedDocs = dataReader.getMultipleDocs(selectedVols, featuresToLoad);
			for (Document doc : selectedDocs) {
				HashMap<String, Double> features = doc.getFeatures();
				for (String key : features.keySet()) {
					int newCount = features.get(key).intValue();
					int existingCount = wordcounts.get(key);
					if (existingCount == 0) wordcounts.put(key, newCount);
					else wordcounts.put(key, existingCount + newCount);
					// really that's a sort of moot if-else since 0 + newCount = newCount
				}
			}
		}
			
		// now we have a HashMap that maps each word to its count
		ValueComparator inValueOrder = new ValueComparator(wordcounts);
		int mapSize = wordcounts.size();
		String[] allTheKeys = new String[mapSize];
		int idx = 0;
		for (String aWord : wordcounts.keySet()) {
			allTheKeys[idx] = aWord;
			idx += 1;
		}
		System.out.println("Sorting vocabulary.");
		Arrays.sort(allTheKeys, inValueOrder);
		
		ArrayList<String> orderedVocabulary = new ArrayList<String>();
		
		for (int i = 0; i < vocabularySize; ++i) {
			orderedVocabulary.add(allTheKeys[i]);
		}
		
		wordcounts = null;
		allTheKeys = null;
		System.gc();
		return orderedVocabulary;
	}

}
