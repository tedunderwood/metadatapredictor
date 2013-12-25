package classification;

import java.util.ArrayList;

import weka.classifiers.Classifier;
// import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.Instance;


public class LogisticClassifier extends SupervisedLearner {
	
	Classifier logistic;
	Instances trainingSet;
	ArrayList<String> features;
	FastVector featureNames;
	int numFeatures;
	int numInstances;
	String ridgeParameter;
	String classLabel;
	double[][] memberProbs;
	
	public LogisticClassifier(String genreToIdentify, ArrayList<Double> classLabels, ArrayList<String> features, 
			ArrayList<ArrayList<Double>> docFeatureValues, String ridgeParameter) {
		
		boolean verbose = Global.verbose;
		// It's a bit of a hack, but we store a flag indicating how verbosely to log events in the
		// static final class Global.
		
		numFeatures = features.size();
		numInstances = classLabels.size();
		this.ridgeParameter = ridgeParameter;
		this.classLabel = genreToIdentify;
		this.features = features;
		memberProbs = new double[numInstances][2];
		
		featureNames = new FastVector(numFeatures + 1);
		for (int i = 0; i < numFeatures; ++ i) {
			Attribute a = new Attribute(features.get(i));
			featureNames.addElement(a);
		}
		
		// Now we add the class attribute.
		FastVector classValues = new FastVector(2);
		classValues.addElement("positive");
		classValues.addElement("negative");
		Attribute classAttribute = new Attribute("class", classValues);
		featureNames.addElement(classAttribute);
		
		trainingSet = new Instances(genreToIdentify, featureNames, numInstances);
		trainingSet.setClassIndex(numFeatures);
		ArrayList<Instance> simpleListOfInstances = new ArrayList<Instance>(numInstances);
		
		int poscount = 0;
		for (int h = 0; h < numInstances; ++ h) {
			ArrayList<Double> aDoc = docFeatureValues.get(h);
			Instance instance = new Instance(numFeatures + 1);
			for (int i = 0; i < numFeatures; ++i) {
				instance.setValue((Attribute)featureNames.elementAt(i), aDoc.get(i));
			}
			if (classLabels.get(h) > 0.5) {
				// this is a positive instance
				instance.setValue((Attribute)featureNames.elementAt(numFeatures), "positive");
				poscount += 1;
			}
			else {
				instance.setValue((Attribute)featureNames.elementAt(numFeatures), "negative");
			}
			trainingSet.add(instance);
			simpleListOfInstances.add(instance);
		}
		
		if (verbose) {
			WarningLogger.logWarning(genreToIdentify + " count: " + poscount + "\n");
		}
		
		try {
			String[] options = {"-R", ridgeParameter};
			logistic = Classifier.forName("weka.classifiers.functions.Logistic", options);
			logistic.buildClassifier(trainingSet);
			if (verbose) {
				WarningLogger.logWarning(logistic.toString());
			}
			 
//			Evaluation eTest = new Evaluation(trainingSet);
//			eTest.evaluateModel(logistic, trainingSet);
//			 
//			String strSummary = eTest.toSummaryString();
//			if (verbose) {
//				WarningLogger.logWarning(strSummary);
//			}
//			
//			for (int i = 0; i < numInstances; ++i) {
//				Instance anInstance = simpleListOfInstances.get(i);
//				memberProbs[i] = logistic.distributionForInstance(anInstance);
//			}
//			// Get the confusion matrix
//			double[][] cmMatrix = eTest.confusionMatrix();
//			if (verbose) {
//				WarningLogger.logWarning("      Really " + genreToIdentify + "     other.");
//				WarningLogger.logWarning("===================================");
//				String[] lineheads = {"ID'd " + genreToIdentify+ ":  ", "ID'd other "};
//				for (int i = 0; i < 2; ++i) {
//					double[] row = cmMatrix[i];
//					WarningLogger.logWarning(lineheads[i] + Integer.toString((int) row[0]) + "             " + Integer.toString((int) row[1]));
//				}
//			}
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println(e);
		}
		if (verbose) {
			WarningLogger.logWarning("\n\n");
		}
		
	}
	
	public double[][] getPredictions() {
		return memberProbs;
	}
	
	public ArrayList<Double> testNewInstances(ArrayList<Document> pointsToTest) {
		ArrayList<Double> testProbs = new ArrayList<Double>();
		
		for (Document doc : pointsToTest) {
			testProbs.add(predictDocument(doc));
		}
		
		return testProbs;
	}
	
	public double predictDocument(Document instance) {
		ArrayList<Double> vector = new ArrayList<Double>();
		for (String term : features) {
			vector.add(instance.getNormalizedTermFreq(term));
		}
		double[] prediction = predictVector(vector);
		return prediction[0];
	}
	
	public double[] predictVector(ArrayList<Double> vector) {
		assert (numFeatures == vector.size());
		
		double[] test = new double[2];
		Instance instance = new Instance(numFeatures + 1);
		instance.setDataset(trainingSet);
		for (int i = 0; i < numFeatures; ++i) {
			instance.setValue((Attribute)featureNames.elementAt(i), vector.get(i));
		}
		
		// setting the classLabel is arbitrary and may not be necessary
		instance.setValue((Attribute)featureNames.elementAt(numFeatures), "negative");
	
		try{
			test = logistic.distributionForInstance(instance);
		}
		catch (Exception e) {
			System.out.println(e);
		}
		
		return test;
	}
	
	public double predictScalar(double value) {
		assert (numFeatures == 1);
		
		ArrayList<Double> vector = new ArrayList<Double>(1);
		vector.add(value);
		double[] prediction = predictVector(vector);
		return prediction[0];
	}
	

}


