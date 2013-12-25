package deduplication;

public class Connection implements Comparable<Connection> {
	Summary first;
	Summary second;
	public double cossim;
	double titlematch;
	double authormatch;
	
	public Connection(Summary first, Summary second, double cossim, double titlematch, double authormatch) {
		this.first = first;
		this.second = second;
		this.cossim = cossim;
		this.titlematch = titlematch;
		this.authormatch = authormatch;
	}
	
	public String outputLine() {
		String outputLine = first.label + "\t" + second.label + "\t" + Double.toString(cossim) +
				"\t" + Double.toString(titlematch) + "\t" + Double.toString(authormatch) + "\n";
		return outputLine;
	}
	
	public int compareTo(Connection comparableConnection) {
		double difference = comparableConnection.cossim - this.cossim;
		// that should implement descending order
		if (difference < 0) return -1;
		if (difference == 0) return 0;
		else return 1;
	}
}

