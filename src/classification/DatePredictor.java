package classification;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import datasets.*;

public class DatePredictor {
	
	static Metadata metadata;

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
		String[] fieldList = {"date"};
		int binRadius = Integer.parseInt(args[2]);
		int maxVolsToRead = Integer.parseInt(args[3]);
		
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
		
		DateClassMap classMap = new DateClassMap(firstBinMidpoint, endDate, binRadius,
			binSpacing, metadata, "date");
		classMap.mapVolsByMetadata();
		
		ArrayList<String> classLabels = classMap.getValidClasses();
		
		int minClassSize = 10000000;
		for (String label : classLabels) {
			int thisSize = classMap.getClassSize(label);
			if (thisSize < minClassSize) minClassSize = thisSize;
		}
		
		SparseTableReader dataReader = new SparseTableReader(dataSource);
		
		// Let's build a vocabulary
		int volumesPerClass = 100;
		for (String label : classLabels) {
			int volumesToGet;
			int thisSize = classMap.getClassSize(label);
			if (thisSize < volumesPerClass) volumesToGet = thisSize;
			else volumesToGet = volumesPerClass;
			
		}
		
		// Now we're going to build a model for each class in the classMap, and save it.
		// 
		Map<String, HashMap<String, Integer>> wordcounts = new HashMap<String, HashMap<String, Integer>>();
		try {
			 wordcounts = dataReader.readTSVasMap();
		}
		catch (InputFileException e) {
			String stacktrace = stacktraceToString(e);
			System.out.println("Exception in dataReader\n" + stacktrace);
			System.exit(0);
		}
		System.out.println("Done loading data.");
		System.out.println("Loaded " + Integer.toString(wordcounts.size()) + " volume IDs as data.");
	}
	
	private static String stacktraceToString(InputFileException e) {
	    return Arrays.toString(e.getStackTrace());
	}

}
