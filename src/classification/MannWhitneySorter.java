package classification;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class MannWhitneySorter {
	Corpus corpus;
	int D;
	
	public MannWhitneySorter(Corpus corpus) {
		this.corpus = corpus;
		D = corpus.numDocuments;
	}
	
	public int[] rankTermsByMW(ArrayList<String> terms) {
		int V = terms.size();
		double[] rhoValues = new double[V];
		
		ArrayList<Double> memberProbs = corpus.getMembershipProbs();
		assert (D == memberProbs.size());
		
		// Following code is a kloodge because this class was originally written to use
		// arrays rather than ArrayLists.
		
		double[] memberDegrees = new double[D];
		for (int i = 0; i < D; ++i) {
			memberDegrees[i] = memberProbs.get(i);
		}
		
		for (int i = 0; i < V; ++ i) {
			String thisterm = terms.get(i);
			ArrayList<Double> distributionOverDocs = corpus.normalizedFreqOverDocs(thisterm);
			
			// Again, a kloodge because this class was originally written to use arrays.
			double[] distribution = new double[D];
			for (int j = 0; j < D; ++j) {
				distribution[j] = distributionOverDocs.get(j);
			}
			rhoValues[i] = mannWhitney(memberDegrees, distribution);	
		}
		
		IntDoublePair[] termRanks = new IntDoublePair[V];
		for (int i = 0; i < V; i ++) {
			termRanks[i] = new IntDoublePair(rhoValues[i], i);
		}
		
		Arrays.sort(termRanks);
		// At this point termRanks is an array of IntDoublePairs, 
		// sorted by the Double part (the rho value). The int part
		// contains the original position of that rho value in the
		// term array, so if you peel off the int part you get
		// an array of indexes to the termtype array, sorted in rho
		// order. Note that since Arrays.sort produces ascending
		// order, we need to reverse the loop.
		
		int[] sortedTermIndices = new int[V];
		for (int i = termRanks.length - 1, j = 0; i > -1; i--, j++) {
			sortedTermIndices[j] = termRanks[i].getPosition();
		}
		return sortedTermIndices;
	}
	
	private double mannWhitney(double[] degreeInClass, double[] termFreq) {
		assert (degreeInClass.length == D);
		assert (termFreq.length == D);
		
		// Array recording initial positions of data to be ranked.
        IntDoublePair[] ranks = new IntDoublePair[D];
        for (int i = 0; i < D; i++) {
            ranks[i] = new IntDoublePair(termFreq[i], i);
        }
 
		// Sort the array.
        Arrays.sort(ranks);
        
        // Walk the sorted array, filling output array using sorted positions,
        // resolving ties as we go
        double[] out = new double[D];
        int pos = 1;  // position in sorted array
        out[ranks[0].getPosition()] = pos;
        List<Integer> tiesTrace = new ArrayList<Integer>();
        tiesTrace.add(ranks[0].getPosition());
        for (int i = 1; i < D; i++) {
            if (Double.compare(ranks[i].getValue(), ranks[i - 1].getValue()) > 0) {
                // tie sequence has ended (or had length 1)
                pos = i + 1;
                if (tiesTrace.size() > 1) {  // if seq is nontrivial, resolve
                    resolveTie(out, tiesTrace);
                }
                tiesTrace = new ArrayList<Integer>();
                tiesTrace.add(ranks[i].getPosition());
            } else {
                // tie sequence continues
                tiesTrace.add(ranks[i].getPosition());
            }
            out[ranks[i].getPosition()] = pos;
        }
        if (tiesTrace.size() > 1) {  // handle tie sequence at end
            resolveTie(out, tiesTrace);
        }
        
        // Now out contains an array of integers whose position corresponds to the
        // position of the original termFreq, but which contains, instead of termFreq,
        // the rank of each termFreq in the original array, beginning at 1, and with
        // tied values averaged.
        // If class membership were a binary proposition we might now compute U by
        // summing the ranks of in-class and out-class samples. But it may not be 
        // a binary proposition, so we multiply by a real number 0-1 that represents
        // degree of class membership.
        
        double inSum = 0d;
        double n1 = 0d;
        
        for (int i = 0; i < D; ++i) {
        	inSum += out[i] * degreeInClass[i];
        	n1 += degreeInClass[i];			
        }
        
        // Now, normally Mann-Whitney rho is the sum of ranks in a class, divided
        // by the product of the sample sizes in both classes. Here, we adapt this
        // for continuous rather than disjoint classes. We know that the out-of-class
        // membership degree is 1 - degreeInClass, so multiplying for a vector of length
        // D, we have
        
        double n2 = D - n1;
        double rho = inSum / (n1 * n2);
        return rho;
	}
	
	 /**
     * Resolve a sequence of ties, by averaging their ranks
     * The input <code>ranks</code> array is expected to take the same value
     * for all indices in <code>tiesTrace</code>.
     *
     * @param ranks array of ranks
     * @param tiesTrace list of indices where <code>ranks</code> is constant
     * -- that is, for any i and j in TiesTrace, <code> ranks[i] == ranks[j]
     * </code>
     */
	private void resolveTie(double[] newranks, List<Integer> tiesTrace) {
        // constant value of ranks over tiesTrace
        final double c = newranks[tiesTrace.get(0)];

        // length of sequence of tied ranks
        final int length = tiesTrace.size();
        Iterator<Integer> iterator = tiesTrace.iterator();
        double averageValue = (2 * c + length - 1) / 2d;
        while (iterator.hasNext()) {
        	newranks[iterator.next()] = averageValue;
        }
    }

	
    private static class IntDoublePair implements Comparable<IntDoublePair>  {

        /** Value of the pair */
        private final double value;

        /** Original position of the pair */
        private final int position;

        /**
         * Construct an IntDoublePair with the given value and position.
         * @param value the value of the pair
         * @param position the original position
         */
        public IntDoublePair(double value, int position) {
            this.value = value;
            this.position = position;
        }

        /**
         * Compare this IntDoublePair to another pair.
         * Only the <strong>values</strong> are compared.
         *
         * @param other the other pair to compare this to
         * @return result of <code>Double.compare(value, other.value)</code>
         */
        public int compareTo(IntDoublePair other) {
            return Double.compare(value, other.value);
        }

        // N.B. equals() and hashCode() are not implemented; see MATH-610 for discussion.

        /**
         * Returns the value of the pair.
         * @return value
         */
        public double getValue() {
            return value;
        }

        /**
         * Returns the original position of the pair.
         * @return position
         */
        public int getPosition() {
            return position;
        }
    }
    
}
