package classification;

import java.io.*;
import java.util.Vector;

public class LineReader {
	File fileName;
	
public LineReader(String dirPath) {
	this.fileName = new File(dirPath);
}

public String[] readlines() throws InputFileException {
	try{
		BufferedReader filein = new BufferedReader(new FileReader(fileName));
		Vector<String> holding = new Vector<String>(1000,1000);
		int count = 0;
		try{
			while (filein.ready()){
				String line = filein.readLine();
				holding.add(line);
				++ count;
			}
		}
		finally {
			filein.close();
		}
		String[] lineArray = new String[count];
		for (int i = 0; i < count; ++i) {
			String line = holding.get(i);
			lineArray[i] = line;
		}
		return lineArray;
	}
	catch (IOException e){
		InputFileException problem = new InputFileException("IOException in LineReader: " + e);
		throw problem;
	}
}

}