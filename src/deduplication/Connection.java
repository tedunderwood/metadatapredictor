package deduplication;

public class Connection implements Comparable<Connection> {
	Summary first;
	Summary second;
	public double cossim;
	double titlematch;
	double authormatch;
	double probability;
	
	public Connection(Summary first, Summary second, double cossim, double titlematch, double authormatch) {
		this.first = first;
		this.second = second;
		this.cossim = cossim;
		this.titlematch = titlematch;
		this.authormatch = authormatch;
		probability = calculateProbability();
	}
	
	private double calculateProbability() {
		// This applies coefficients learned through logistic regression.
		double exponent = (56.589 * cossim) - 55.559;
		return 1 / (1 + Math.exp(-exponent));
		// that's the logit function
	}
	
	public String outputLine() {
		String firstSize = Integer.toString(first.numWords);
		String secondSize = Integer.toString(second.numWords);
		String distance = Double.toString((first.numWords - second.numWords) / ((first.numWords + second.numWords) / 2d) );
		String outputLine = first.label + "\t" + firstSize + "\t" + second.label + "\t" + secondSize + "\t" + Double.toString(cossim) +
				"\t" + distance + "\t" + Double.toString(titlematch) + "\t" + Double.toString(authormatch) + "\t" + first.title + 
				"\t" + second.title;
		return outputLine;
	}
	
	public int compareTo(Connection comparableConnection) {
		double difference = comparableConnection.probability - this.probability;
		// that should implement descending order
		if (difference < 0) return -1;
		if (difference == 0) return 0;
		else return 1;
	}
}

