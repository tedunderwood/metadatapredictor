package deduplication;

import java.util.ArrayList;

import datasets.Volume;

public class Summary {
	public String label;
	double[] features;
	ArrayList<String> volumesInRecord;
	public int numWords;
	String title;
	public String author;
	String recordID;
	int pointer;
	
	public Summary(Volume vol, double[] vector) {
		features = vector;
		label = vol.htid;
		title = vol.getValue("title");
		author = vol.getValue("author");
		recordID = vol.getValue("recordid");
		numWords = vol.getNumWords();
		volumesInRecord = new ArrayList<String>(1);
		volumesInRecord.add(label);
	}
	
	public void setPointer(int value){
		pointer = value;
	}
	
	public Summary(String label, int numFeatures, ArrayList<Summary> volumeList) {
		this.label = label;
		features = new double[numFeatures];
		volumesInRecord = new ArrayList<String>(numFeatures);
		numWords = 0;
		
		for (Summary vol : volumeList) {
			double[] nextVector = vol.getFeatures();
			for (int i = 0; i < numFeatures; ++i) {
				features[i] += nextVector[i];
			}
			numWords += vol.getNumWords();
			volumesInRecord.add(vol.label);
		}
		
		Summary firstVol = volumeList.get(0);
		title = firstVol.title;
		author = firstVol.author;
		recordID = firstVol.recordID;
		assert (recordID == label);
	}
	
	public String getRecordID() {
		return recordID;
	}
	
	public double[] getFeatures() {
		return features;
	}
	
	public int getNumWords() {
		return numWords;
	}
	
	public String normalizedTitle() {
		return title.toLowerCase();
	}
	
	public String normalizedAuthor() {
		return author.toLowerCase();
	}
	
	public String outputName() {
		return (label + "\t" + author + "\t" + title);
	}
}
