package deduplication;

public class Connection implements Comparable<Connection> {
	Summary first;
	Summary second;
	public double cossim;
	double distance;
	double probability;
	
	public Connection(Summary first, Summary second, double cossim, double distance) {
		this.first = first;
		this.second = second;
		this.cossim = cossim;
		this.distance = distance;
		probability = calculateProbability(cossim, distance);
	}
	
	public Connection(Summary first, Summary second) {
		this.first = first;
		this.second = second;
		this.cossim = cosineSimilarity(first.getFeatures(), second.getFeatures());
	}
	
	public double calculateProbability() {
		this.distance = euclideanDistance(first.rawfeatures, second.rawfeatures);
		this.probability = calculateProbability(cossim, distance/10000);
		// Dividing by ten thousand just to get more manageable coefficients. Kludgy, I know.
		return probability;
	}
	
	private static double calculateProbability(double cos, double dist) {
		// This applies coefficients learned through logistic regression.
		// Empirically, cosine similarity is more useful than distance.
		// Title similarity is a useful empirical clue overall, but I don't use it
		// because it tends to create problematic superclusters called e.g.
		// "Waverley novels."
		double exponent = (103.993 * cos) - (9.021 * dist) - 100.013;
		return 1 / (1 + Math.exp(-exponent));
		// that's the logit function
	}
	
	public String outputLine() {
		int firstSize = first.numWords;
		int secondSize = second.numWords;
		String outputLine = first.label + "\t" + firstSize + "\t" + second.label + "\t" + secondSize + "\t" + cossim +
				"\t" + distance + "\t" + probability + "\t" + first.title + "\t" + second.title;
		return outputLine;
	}
	
	public int compareTo(Connection comparableConnection) {
		double difference = comparableConnection.probability - this.probability;
		// that should implement descending order
		if (difference < 0) return -1;
		if (difference == 0) return 0;
		else return 1;
	}
	
	private static double cosineSimilarity(double[] first, double[] second) {
		int vectorLength = first.length;
		assert(first.length == second.length);
		double dotProduct = 0d;
		double firstMagnitude = 0d;
		double secondMagnitude = 0d;
		for (int i = 0; i < vectorLength; ++i){
			dotProduct += first[i] * second[i];
			firstMagnitude += first[i] * first[i];
			secondMagnitude += second[i] * second[i];
		}
		firstMagnitude = Math.sqrt(firstMagnitude);
		secondMagnitude = Math.sqrt(secondMagnitude);
		double denominator = (firstMagnitude * secondMagnitude);
		if (denominator < 0.1) {
			return 0d;
			// The logic here is twofold. A) We want to avoid division by zero.
			// More importantly B) We want to ignore very short documents, or
			// documents lacking English words.
		}
		else {
			return dotProduct / denominator;
		}
	}
	
	private static double euclideanDistance(double[] first, double[] second) {
		int vectorLength = first.length;
		assert(second.length == vectorLength);
		
		double sumOfSquares = 0d;
		
		for (int i = 0; i < vectorLength; ++i) {
			sumOfSquares += Math.pow((first[i] - second[i]), 2);
		}
		
		return Math.sqrt(sumOfSquares);
	}
}

