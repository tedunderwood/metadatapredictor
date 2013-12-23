package classification;
import java.util.ArrayList;

import datasets.ClassMap;

/**
 * Abstract class defining an interface that should be implemented
 * by concrete Trainers for specific applications.
 * 
 * @author tunderwood
 * @version 1.0
 * @since 2013-12-19
 */
public abstract class Trainer {
	ArrayList<SupervisedLearner> ensemble;
	String dataPath;
	int numFeaturesToTest;
	int numFeaturesToSelect;
	ClassMap classMap;
	ArrayList<ArrayList<String>> featuresPerClassifier;
}
