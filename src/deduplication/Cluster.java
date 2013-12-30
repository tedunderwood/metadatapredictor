package deduplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class Cluster implements Comparable<Cluster> {
	
	int sizeOfCluster;
	double coherence;
	HashSet<Summary> thisCluster;
	
	public Cluster(HashSet<Summary> aCluster) {
		thisCluster = aCluster;
		sizeOfCluster = aCluster.size();
		coherence = this.crossCompare();
	}
	
	private double crossCompare(){
		
		if (sizeOfCluster < 2) return 1d;
		
		Iterator<Summary> iter1 = thisCluster.iterator();

		ArrayList<Double> allLinkages = new ArrayList<Double>();
		
		while (iter1.hasNext()) {
			Summary outer = iter1.next();
			Iterator<Summary> iter2 = thisCluster.iterator();
			
			while (iter2.hasNext()) {
				Summary inner = iter2.next();
				if (outer.equals(inner)) continue;
				Connection tentativeConnection = new Connection(outer, inner);
				Double probability = tentativeConnection.calculateProbability();
				allLinkages.add(probability);
			}
		}	
		
		double sumProb = 0d;
		for (Double linkProb : allLinkages) {
			sumProb += linkProb;
		}
		
		double result = sumProb / (double) allLinkages.size();

		return result;
	}

	public int compareTo(Cluster comparableCluster) {
		double difference = comparableCluster.coherence - this.coherence;
		// that should implement descending order
		if (difference < 0) return -1;
		if (difference == 0) return 0;
		else return 1;
	}
}
