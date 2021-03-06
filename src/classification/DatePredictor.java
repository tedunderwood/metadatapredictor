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
	static int SMOOTHSPAN = 12;
	static int VOLUMESPERCLASSFORLEXICON = 20;

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
		
		startDate = 1800;
		endDate = 1899;
		// both boolean flags are true because we tolerate errors in date parsing
		
		System.out.println("Start date: " + Integer.toString(startDate));
		System.out.println("End date: " + Integer.toString(endDate));
		
		if (endDate < 0 | startDate < 0) {
			System.out.println("Bad dates!");
			System.exit(1);
		}
		int firstBinMidpoint = startDate + binRadius;
		int binSpacing = (binRadius * 2) + 1;
		// So, for instance, a startDate of 1700 with binRadius of 2 produces
		// firstBinMidpoint 1702 and
		// binSpacing 5
		
		classMap = new DateClassMap(firstBinMidpoint, endDate, binRadius,
			binSpacing, metadata, "date");
		classMap.mapVolsByMetadata(1800, 1899);
		
		System.out.println("Done constructing classMap.");
		
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
		System.out.println("Built vocabulary.");
		
		// Now we're going to build a model for each class in the classMap, and save it.
		int maxSetSize = maxVolsToRead / 2;
		int setSize;
		ArrayList<LogisticClassifier> models = new ArrayList<LogisticClassifier>(classCount);
		
		for (String label : classLabels) {
			System.out.println("Building a model for class: " + label);
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
		// We read files in chunks to avoid maxing out memory.
		
		ArrayList<Volume> volumes = metadata.getVolumes();
		int numVolumes = metadata.getSize();
		int numChunks = (int) Math.ceil(numVolumes / (double) maxVolsToRead);
		
		// We store predictions in a collection, but also write them to file when
		// each chunk is completed.
		LineWriter progressiveWriter = new LineWriter(outputFolder + "cumulativePredictions.tsv", true);
		// The boolean flag sets this so that each write will append rather than overwrite the file.
		
		ArrayList<ArrayList<Double>> predictAllVols = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < numChunks; ++i) {
			int floor = i * maxVolsToRead;
			int ceiling = (i + 1) * maxVolsToRead;
			if (ceiling > numVolumes) ceiling = numVolumes;
			String[] outputChunk = new String[ceiling-floor];
			int counter = 0;
			for (int j = floor; j < ceiling; ++j) {
				Volume vol = volumes.get(j);
				Document doc = dataReader.getDocument(vol, vocabulary);
				// Now, it's possible that the doc was actually not found by the
				// dataReader. Thus the if-then-else statement inside the loop below.
				
				ArrayList<Double> predictionVector = new ArrayList<Double>();
				for (LogisticClassifier model : models) {
					if (doc.fileNotFound) {
						predictionVector.add(0d);
					}
					else {
						predictionVector.add(model.predictDocument(doc));
					}
				}
				predictAllVols.add(predictionVector);
				outputChunk[counter] = outputLine(doc, predictionVector, classLabels, SMOOTHSPAN);
				counter += 1;
			}
			progressiveWriter.send(outputChunk);
		}
		
		// The lines that follow actually duplicate the output produced by progressiveWriter,
		// but they do so with some valuable niceties, like a header.
		
		ArrayList<String> attestedDates = new ArrayList<String>();
		ArrayList<Integer> predictedDates = new ArrayList<Integer>();
		// Now we need to infer estimated dates from the maximum prediction. But since
		// predictions may be noisy, we want to do this with some smoothing.
		for (int i = 0; i < numVolumes; ++i) {
			int predictedDate = predictDate(predictAllVols.get(i), classLabels, SMOOTHSPAN);
			String attestedDate = volumes.get(i).getValue("date");
			predictedDates.add(predictedDate);
			attestedDates.add(attestedDate);
			System.out.println(predictedDate);
		}
		
		ArrayWriter volumePredictions = new ArrayWriter("\t");
		ArrayList<String> htids = new ArrayList<String>();
		for (Volume vol : volumes) {
			htids.add(vol.htid);
		}
		volumePredictions.addStringColumn(htids, "volume");
		volumePredictions.addStringColumn(attestedDates, "attested");
		volumePredictions.addIntegerColumn(predictedDates, "predicted");
		volumePredictions.addDoubleArray(predictAllVols, classLabels);
		volumePredictions.writeToFile(outputFolder + "volumePredictions.tsv");
		
		WarningLogger.writeFilesNotFound(outputFolder + "filesNotFound.txt");
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
	
	private static String outputLine(Document doc, ArrayList<Double> predictionVector, ArrayList<String> classLabels, int span) {
		String predictedDate;
		if (doc.fileNotFound) {
			predictedDate = "0";
		}
		else {
			predictedDate = Integer.toString(predictDate(predictionVector, classLabels, span));
		}
		String attestedDate = doc.getVolume().getValue("date");
		String htid = doc.getVolume().htid;
		String allPredictions = "";
		int columns = predictionVector.size();
		for (int j = 0; j < columns; ++ j) {
			allPredictions = allPredictions + predictionVector.get(j);
			if (j < (columns-1)) {
				allPredictions = allPredictions + "\t";
			}
		}
		String outLine = htid + "\t" + attestedDate + "\t" + predictedDate + "\t" + allPredictions;
		return outLine;	
	}
	
	private static String stacktraceToString(InputFileException e) {
	    return Arrays.toString(e.getStackTrace());
	}
	
	private static ArrayList<String> buildVocabulary(ArrayList<String> classLabels, int vocabularySize, PairtreeReader dataReader) {
		// Let's build a vocabulary
		HashSet<String> featuresToLoad = new HashSet<String>();
		// We leave this empty, which ensures loading all features.
		HashMap<String, Integer> wordcounts = new HashMap<String, Integer>();
		
		System.out.println("Building vocabulary.");
		for (String label : classLabels) {
			System.out.println("Getting class " + label + ".");
			int volumesToGet;
			int thisSize = classMap.getClassSize(label);
			if (thisSize < VOLUMESPERCLASSFORLEXICON) volumesToGet = thisSize;
			else {
				volumesToGet = VOLUMESPERCLASSFORLEXICON;
			}
			ArrayList<Volume> selectedVols = classMap.takeRandomSample(label, volumesToGet);
			System.out.println("Contains " + Integer.toString(selectedVols.size()) + " volumes.");
			ArrayList<Document> selectedDocs = dataReader.getMultipleDocs(selectedVols, featuresToLoad);
			for (Document doc : selectedDocs) {
				HashMap<String, Double> features = doc.getFeatures();
				for (String key : features.keySet()) {
					int newCount = features.get(key).intValue();
					
					if (wordcounts.containsKey(key)) {
						int existingCount = wordcounts.get(key);
						wordcounts.put(key, existingCount + newCount);
					}
					else {
						wordcounts.put(key, newCount);
					}
				}
			}
		}
			
		// now we have a HashMap that maps each word to its count
		ValueComparator inValueOrder = new ValueComparator(wordcounts);
		int mapSize = wordcounts.size();
		System.out.println("Total dictionary size: " + Integer.toString(mapSize));
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

		return orderedVocabulary;
	}

}
