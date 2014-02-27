package classification;

import java.util.HashSet;

/**
 * @author tunder
 * @version 1.0
 * @since 2013-12-18
 *
 */
public final class WarningLogger {
	static LineWriter theWriter;
	static boolean writeToFile = false;
	static HashSet<String> notFound = new HashSet<String>();
	
	public static void initializeLogger(boolean toFile, String filename) {
		writeToFile = toFile;
		if (writeToFile) {
			theWriter = new LineWriter(filename, true);
		}
	}
	
	public static void logWarning(String theWarning) {
		if (writeToFile) {
			theWriter.print(theWarning);
		}
		else {
			System.out.println(theWarning);
		}
	}
	
	public static void addFileNotFound(String file) {
		notFound.add(file);
	}
	
	public static void writeFilesNotFound(String path) {
		LineWriter outFile = new LineWriter(path, false);
		String[] outLines = new String[notFound.size()];
		int counter = 0;
		for (String aFile : notFound) {
			outLines[counter] = aFile;
			counter += 1;
		}
		outFile.send(outLines);
	}

}
