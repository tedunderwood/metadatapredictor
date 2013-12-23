package classification;
import java.util.ArrayList;
import java.util.HashSet;

import datasets.Volume;

public abstract class Corpus {
	String classLabel = "__blank__";
	// The class the corpus is constructed to discern. When training corpora
	// are created, it's reset to a particular classLabel. When corpora are
	// created for actual classification, this can be left __blank__.
	ArrayList<Document> documents;
	ArrayList<Double> classMembership;
	ArrayList<Volume> volumes;
	HashSet<String> featuresToLoad;
	int numDocuments;
	int numFeatures;
	
	public abstract ArrayList<Document> getAllInstances();
	
	public abstract ArrayList<Double> normalizedFreqOverDocs(String term);
	
	public abstract ArrayList<Double> getMembershipProbs();
	
	public abstract ArrayList<Double> rawFreqOverDocs(String term);
}
