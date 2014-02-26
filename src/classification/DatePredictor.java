package classification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.*;

import datasets.*;

public class DatePredictor {
	
	static Metadata metadata;
	static DateClassMap classMap;
	static int startDate;
	static int endDate;
	static int binRadius;

	/**
	 * @author tunderwood
	 * @version 1.0
	 * @since 2014-2-23
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String metadataFile = args[0];
		String dataFolder = args[1];
		String[] fieldList = {"date"};
		binRadius = Integer.parseInt(args[2]);
		int vocabularySize = Integer.parseInt(args[3]);
		int maxVolsToRead = Integer.parseInt(args[4]);
		String ridgeParameter = args[5];
		String outputFolder = args[6];
		
		WarningLogger.initializeLogger(true, outputFolder + "errorlog.txt");
		
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
		
		startDate = metadata.getMinDate(true);
		endDate = metadata.getMaxDate(true);
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
		
		PairtreeReader dataReader = new PairtreeReader(dataFolder);
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
			// We have added the classifier to a collection of models. Now we serialize it and
			// write it to file so we can reconstruct this process if needed.
			try {
				FileOutputStream fileout = new FileOutputStream(outputFolder + label + ".classifier");
				ObjectOutputStream serializer = new ObjectOutputStream(fileout);
				serializer.writeObject(thisClassifier);
				serializer.close();
				fileout.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		// Now we actually classify the volumes using our model.
		ArrayList<Volume> volumes = metadata.getVolumes();
		int numVolumes = metadata.getSize();
		int numChunks = (int) Math.ceil(numVolumes / (double) maxVolsToRead);
		ArrayList<ArrayList<Double>> predictAllVols = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < numChunks; ++i) {
			int floor = i * maxVolsToRead;
			int ceiling = (i + 1) * maxVolsToRead;
			if (ceiling > numVolumes) ceiling = numVolumes;
			for (int j = floor; j < ceiling; ++j) {
				Volume vol = volumes.get(j);
				Document doc = dataReader.getDocument(vol, vocabulary);
				ArrayList<Double> predictionVector = new ArrayList<Double>();
				for (LogisticClassifier model : models) {
					predictionVector.add(model.predictDocument(doc));
				}
				predictAllVols.add(predictionVector);
			}
		}
		
		ArrayList<Integer> predictedDates = new ArrayList<Integer>();
		// Now we need to infer estimated dates from the maximum prediction. But since
		// predictions may be noisy, we want to do this with some smoothing.
		for (int i = 0; i < numVolumes; ++i) {
			int newDate = predictDate(predictAllVols.get(i), classLabels, 12);
			predictedDates.add(newDate);
		}
		
		ArrayWriter volumePredictions = new ArrayWriter("\t");
		ArrayList<String> htids = new ArrayList<String>();
		for (Volume vol : volumes) {
			htids.add(vol.htid);
		}
		volumePredictions.addStringColumn(htids, "volume");
		volumePredictions.addIntegerColumn(predictedDates, "date");
		volumePredictions.addDoubleArray(predictAllVols, classLabels);
		volumePredictions.writeToFile(outputFolder + "volumePredictions.tsv");
	}
	
	private static int predictDate(ArrayList<Double> predictionVector, ArrayList<String> classLabels, int span) {
		int predictedDate = 0;
		double maxPrediction = 0;
		
		// This is basically a smoothing problem. We have predictions located at bin midpoints, and we want to
		// infer smoothed predictions for specific years.
		
		for (int date = startDate; date <= endDate; ++date) {
			ArrayList<Double> relevanceVector = new ArrayList<Double>();
			for (String label : classLabels) {
				int year = Integer.parseInt(label);
				double relevance;
				if ((year + span) < date | (year - span) > date) relevance = 0d;
				else relevance = (double) (span) - Math.abs(year - date);
				// If you imagine a line from the edge of the span to the date,
				// bisected by the year of this class, this is the far section of
				// the line. So, it's bigger the closer year is to date.
				relevanceVector.add(relevance);
			}
			
			// we normalize the relevanceVector to unit length
			double vectorSum = 0;
			for (Double value : relevanceVector) {
				vectorSum += value;
			}
			for (int i = 0; i < relevanceVector.size(); ++ i) {
				relevanceVector.set(i, relevanceVector.get(i) / vectorSum);
			}
			
			double thisPrediction = 0;
			for (int i = 0; i < predictionVector.size(); ++i) {
				thisPrediction += predictionVector.get(i) * relevanceVector.get(i);
			}
			if (thisPrediction > maxPrediction) {
				maxPrediction = thisPrediction;
				predictedDate = date;
			}
		}
		
		return predictedDate;
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
