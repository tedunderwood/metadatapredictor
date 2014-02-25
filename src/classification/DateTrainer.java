package classification;

import java.util.ArrayList;
import java.util.HashSet;

import datasets.DateClassMap;
import datasets.Volume;

/**
 * @author tunderwood
 *
 */
public class DateTrainer extends Trainer {
	ArrayList<SupervisedLearner> ensemble;
	String dataPath;
	int numFeaturesToTest;
	DateClassMap classMap;
	ArrayList<ArrayList<String>> featuresPerClassifier;
	HashSet<String> featuresToLoad;
	
	public DateTrainer(String dataPath, int numFeaturesToTest, int numFeaturesToSelect, DateClassMap classMap, HashSet<String> featuresToLoad) {
		this.dataPath = dataPath;
		this.numFeaturesToTest = numFeaturesToTest;
		this.classMap = classMap;
		this.featuresToLoad = featuresToLoad;
	}
	
	public ArrayList<SupervisedLearner> trainAllClasses() {
		ensemble = new ArrayList<SupervisedLearner>();
		ArrayList<String> classes = classMap.getKnownClasses();
		
		for (String thisClass : classes) {
			ArrayList<Volume> positives = classMap.getMembers(thisClass);
			ArrayList<Volume> negatives = classMap.getSelectedNonmembers(thisClass, positives.size());
			VolumeCorpus corpus = new VolumeCorpus(positives, negatives, dataPath, featuresToLoad, thisClass);
			ArrayList<String> featureList = selectFeatures(corpus.getPositives(), corpus.getNegatives(), corpus, numFeaturesToTest);
			SupervisedLearner newClassifier = new BayesianClassifier(featureList, corpus);
			ensemble.add(newClassifier);
		}
		return ensemble;
	}
	
	private ArrayList<String> selectFeatures(ArrayList<Document> positives, ArrayList<Document> negatives, Corpus corpus, int numToTest) {
		ArrayList<String> features = new ArrayList<String>();
		ArrayList<String> allFeatures = new ArrayList<String>();
		for (String aFeature: featuresToLoad) {
			allFeatures.add(aFeature);
		}
		MannWhitneySorter sorter = new MannWhitneySorter(corpus);
		int[] featureOrder = sorter.rankTermsByMW(allFeatures);
		for (int i = 0; i < numToTest; ++i) {
			features.add(allFeatures.get(featureOrder[i]));
		}
		return features;
	}
}
