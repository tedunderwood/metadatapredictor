package classification;

/**
 * @author tunder
 * @version 1.0
 * @since 2013-12-18
 *
 */
public class WarningLogger {
	LineWriter theWriter;
	boolean writeToFile = false;
	
	public WarningLogger(boolean writeToFile, String filename) {
		this.writeToFile = writeToFile;
		if (writeToFile) {
			theWriter = new LineWriter(filename, true);
		}
	}
	
	public void logWarning(String theWarning) {
		if (writeToFile) {
			theWriter.print(theWarning);
		}
		else {
			System.out.println(theWarning);
		}
	}

}
