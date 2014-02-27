package classification;

import java.util.ArrayList;

public class ArrayWriter {
	String separator;
	int rows;
	int columns;
	ArrayList<ArrayList<String>> cells;
	ArrayList<String> header;
	
	public ArrayWriter(String separator) {
		this.separator = separator;
		cells = new ArrayList<ArrayList<String>>();
		header = new ArrayList<String>();
		rows = 0;
		columns = 0;
	}
	
	public void addStringColumn(ArrayList<String> column, String headerLabel) {
		int impliedrows = column.size();
		if (rows == 0 | rows == impliedrows) {
			cells.add(column);
			header.add(headerLabel);
			rows = impliedrows;
			columns += 1;
		}
		else {
			WarningLogger.logWarning("ArrayWriter: adding " + Integer.toString(impliedrows) + " rows to array with "
					+ Integer.toString(rows) + ".");
		}
	}
	
	public void addDoubleColumn(ArrayList<Double> column, String headerLabel) {
		int impliedrows = column.size();
		if (rows == 0 | rows == impliedrows) {
			ArrayList<String> stringColumn = new ArrayList<String>();
			for (Double value : column) {
				stringColumn.add(Double.toString(value));
			}
			cells.add(stringColumn);
			header.add(headerLabel);
			rows = impliedrows;
			columns += 1;
		}
		else {
			WarningLogger.logWarning("ArrayWriter: adding " + Integer.toString(impliedrows) + " rows to array with "
					+ Integer.toString(rows) + ".");
		}
	}
	
	public void addDoubleArray(ArrayList<ArrayList<Double>> doubleArray, ArrayList<String> headerLabels) {
		ArrayList<Double> firstRow = doubleArray.get(0);
		assert firstRow.size() == headerLabels.size();
		int columnsToAdd = firstRow.size();
		
		// The array comes to us organized by row. We need to reorganize it by column.
		ArrayList<ArrayList<Double>> arrayByColumn = new ArrayList<ArrayList<Double>>(columnsToAdd);
		for (int i = 0; i < columnsToAdd; ++ i) {
			ArrayList<Double> newList = new ArrayList<Double>();
			for (int j = 0; j < doubleArray.size(); ++j) {
				newList.add(doubleArray.get(j).get(i));
			}
			arrayByColumn.add(newList);
		}
		
		for (int i = 0; i < arrayByColumn.size(); ++i) {
			addDoubleColumn(arrayByColumn.get(i), headerLabels.get(i));
		}
	}
	
	public void addIntegerColumn(ArrayList<Integer> column, String headerLabel) {
		int impliedrows = column.size();
		if (rows == 0 | rows == impliedrows) {
			ArrayList<String> stringColumn = new ArrayList<String>();
			for (Integer value : column) {
				stringColumn.add(Integer.toString(value));
			}
			cells.add(stringColumn);
			header.add(headerLabel);
			rows = impliedrows;
			columns += 1;
		}
		else {
			WarningLogger.logWarning("ArrayWriter: adding " + Integer.toString(impliedrows) + " rows to array with "
					+ Integer.toString(rows) + ".");
		}
	}
	
	public void writeToFile(String filePath) {
		LineWriter outFile = new LineWriter(filePath, false);
		String [] outLines = new String[rows + 1];
		
		String headerLine = "";
		for (int j = 0; j < columns; ++ j) {
			headerLine = headerLine + header.get(j);
			if (j < (columns-1)) {
				headerLine = headerLine + separator;
			}
		}
		outLines[0] = headerLine;
		
		for (int i = 0; i < rows; ++ i) {
			String thisLine = "";
			for (int j = 0; j < columns; ++ j) {
				thisLine = thisLine + cells.get(j).get(i);
				if (j < (columns-1)) {
					thisLine = thisLine + separator;
				}
			}
			outLines[i + 1] = thisLine;
		}
		outFile.send(outLines);
	}
	
}
