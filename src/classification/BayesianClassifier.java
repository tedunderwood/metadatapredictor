package classification;
import static java.util.Arrays.fill;
import java.util.ArrayList;


/** A naive Bayesian classification model for a particular
 * document category. Permits degrees of class membership,
 * which makes it a little unorthodox.
 * 
 * This class is threadsafe because each instance is owned
 * by one and only one EMfork(thread). It can see only the
 * DocCategory owned by that thread. All threads use the
 * same Corpus, but that is immutable.
 * 
 * @author tunder
 *
 */
public class BayesianClassifier extends SupervisedLearner {
	
	/** The corpus we're classifying. */
	Corpus corpus;
	
	/** Features to be used for classification */
	ArrayList<String> features;
	
	/** The log-likelihood of each feature in the previous array. */
	double[] loglikelihoods;
	
	/** The base probability that a given document will belong to this class, expressed
	 * as a log-likelihood.
	 */
	double baseProbability;
	
	/** The length of the feature set. */
	int featureCount = 0;
	
	/** Number of documents in corpus. */
	int D;
	
	/** Create a Bayesian model to recognize a particular document
	 * category.
	 */
	public BayesianClassifier(ArrayList<String> featureList, Corpus corpus) {
		this.features = featureList;
		featureCount = features.size();
		this.corpus = corpus;
		D = corpus.numDocuments;
		double[] memberDegrees = makeDoubleArray(corpus.getMembershipProbs());
		
		// We're now going to gather the sum of feature counts for all
		// features in the featureset, on the one hand for documents in
		// the class and on the other for those out of the class. Since
		// class membership is a continuous spectrum from 0-1, feature counts
		// will actually be multiplied by degree-of-class-membership.
		double sumAllInClass = 0;
		double sumAllOutClass = 0;
		// These scalar variables contain the sum of *all* features (in the featureset).
		
		double[] inclassSums = new double[featureCount];
		fill(inclassSums,0);
		double[] outclassSums = new double[featureCount];
		fill(outclassSums,0);
		// These vectors contain in- and out-class sums for each feature.
		
		for (int i = 0; i < featureCount; ++ i ) {
			String feature = features.get(i);
			int[] distribOverDocs = doubleListToIntArray(corpus.rawFreqOverDocs(feature));
			for (int d = 0; d < D; ++d){
				double inclass = distribOverDocs[d] * memberDegrees[d];
				// The term's representation in this class, for this document,
				// is the product of its raw frequency in doc and the doc's
				// degree of membership in the class.
				double outclass = distribOverDocs[d] - inclass;
				inclassSums[i] += inclass;
				outclassSums[i] += outclass;
			}
		sumAllInClass += inclassSums[i];
		sumAllOutClass += outclassSums[i];
		}
		
		loglikelihoods = new double[featureCount];
		for (int i = 0; i < featureCount; ++ i ) {
			double probInclass = (inclassSums[i] + 1) / (sumAllInClass + featureCount);
			double probOutclass = (outclassSums[i] + 1) / (sumAllOutClass + featureCount);
			loglikelihoods[i] = Math.log(probInclass / probOutclass);
		}
		
		// We calculate base probability by adding up all the positive probabilities and
		// dividing by the number of instances.
		double sumProbability = 0d;
		for (double d : memberDegrees) {
			sumProbability += d;
		}
		baseProbability = Math.log(sumProbability / memberDegrees.length);
	}
	
	public double predictInstance(Document instance) {
		double prediction = baseProbability;
		//
		for (int f = 0; f < featureCount; ++f) {
			String feature = features.get(f);
			prediction += instance.getRawTermFreq(feature) * loglikelihoods[f];
			// 
		}
		return prediction;
	}
	
private double[] makeDoubleArray(ArrayList<Double> aList) {
	int len = aList.size();
	double[] doubleArray = new double[len];
	for (int i = 0; i < len; ++i) {
		doubleArray[i] = aList.get(i);
	}
	return doubleArray;
}

private int[] doubleListToIntArray(ArrayList<Double> aList) {
	int len = aList.size();
	int[] intArray = new int[len];
	for (int i = 0; i < len; ++i) {
		intArray[i] = (int) Math.round(aList.get(i));
	}
	return intArray;
}
	
}
