package classification;

/**
 * @author tunder
 * @version 1.0
 * @since 2013-12-18
 *
 */
public final class WarningLogger {
	static LineWriter theWriter;
	static boolean writeToFile = false;
	
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

}
