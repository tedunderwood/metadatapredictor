package deduplication;

import java.util.*;

import java.util.ArrayList;

public class HierarchicalClusters {
	ArrayList<Summary> summaries;
	ArrayList<Connection> connections;
	ArrayList<HashSet<Summary>> clusters;
	double CUTOFF = 0.90;
	int SIZEDIFF;
	
	public HierarchicalClusters(ArrayList<Summary> summaries, ArrayList<Connection> connections) {
		clusters = new ArrayList<HashSet<Summary>>();
		this.summaries = summaries;
		SIZEDIFF = RecAndVolCorpus.WINDOW;
		
		// Maps summary objects to clusters. To begin with each summary points to its own index
		// in this list. When a connection is made, one cluster gets emptied and added to the other.
		
		HashSet<String> volumesHaveBeenMoved = new HashSet<String>();
		
		int numSummaries = summaries.size();
		for (int i = 0; i < numSummaries; ++i) {
			HashSet<Summary> newSet = new HashSet<Summary>();
			newSet.add(summaries.get(i));
			clusters.add(newSet);
			summaries.get(i).setPointer(i);
		}
		// So initially, this is simply a copy of summaries.
		
		for (Connection connection : connections) {
			if (connection.probability < CUTOFF) break;
			// Connections are sorted in probability order.
			// We don't want to proceed down the list beyond a
			// preset CUTOFF, defined above.
			
			Summary firstSummary = connection.first;
			int firstPointer = firstSummary.pointer;
			if (firstPointer < 0) continue;
			// see nullifyRecordmembers for the point of this operation
			HashSet<Summary> firstMembers = clusters.get(firstPointer);
			if (firstMembers.size() < 1) {
				System.out.println(firstPointer);
			}
			
			Summary secondSummary = connection.second;
			Integer secondPointer = secondSummary.pointer;
			if (secondPointer < 0) continue;
			// see nullifyRecordmembers for the point of this operation
			HashSet<Summary> secondMembers = clusters.get(secondPointer);
			
			if (firstPointer == secondPointer) continue;
			// don't cluster items already in the same cluster!
			
			if (!okayToMove(firstMembers, volumesHaveBeenMoved)) continue;
			if (!okayToMove(secondMembers, volumesHaveBeenMoved)) continue;
			// The point here is that we don't reassign record objects if
			// any of their volume members have already been reassigned.
			
			// Also, once a record is reclustered, its volumes get reset
			// by nullifyRecordmembers so they point to -1 and cannot
			// subsequently be moved.
			
			if (firstMembers.size() < 2 & secondMembers.size() < 2) {
				Iterator<Summary> iter1 = firstMembers.iterator();
				Summary first = iter1.next();
				Iterator<Summary> iter2 = secondMembers.iterator();
				Summary second = iter2.next();
				secondMembers.clear();
				firstMembers.add(second);
				int newInt = first.pointer;
				second.pointer = newInt;
				volumesHaveBeenMoved.add(first.label);
				volumesHaveBeenMoved.add(second.label);
				nullifyRecordmembers(first);
				nullifyRecordmembers(second);
			}
			else {
				// If the clusters have more than two members we use the "maximum linkage" method to decide whether 
				// to merge them. In other words, the deciding criterion is the *least* probable connection.
				
				double minProb = 1d;
				
				Iterator<Summary> iter1 = firstMembers.iterator();

				ArrayList<Integer> firstSizes = new ArrayList<Integer>();
				ArrayList<Integer> secondSizes = new ArrayList<Integer>();
				
				while (iter1.hasNext()) {
					Summary outer = iter1.next();
					firstSizes.add(outer.numWords);
					Iterator<Summary> iter2 = secondMembers.iterator();
					
					while (iter2.hasNext()) {
						Summary inner = iter2.next();
						Connection temporaryConn = new Connection(outer, inner);
						Double probability = temporaryConn.calculateProbability();
						if (probability < minProb) minProb = probability;
					}
				}
				
				Iterator<Summary> iter2 = secondMembers.iterator();
				while (iter2.hasNext()) {
					Summary thisSum = iter2.next();
					secondSizes.add(thisSum.numWords);
				}
				
				int sumFirstSize = 0;
				for (int size : firstSizes) {
					sumFirstSize += size;
				}
				double meanFirstSize = sumFirstSize / (double)firstSizes.size();
				
				int sumSecondSize = 0;
				for (int size : secondSizes) {
					sumSecondSize += size;
				}
				double meanSecondSize = sumSecondSize / (double)secondSizes.size();
				
				if (minProb < CUTOFF) continue;
				// This is the decision point.
				
				if (Math.abs(meanFirstSize - meanSecondSize) > SIZEDIFF) continue;
				
				else {
					Iterator<Summary> toAdd = secondMembers.iterator();
					while (toAdd.hasNext()) {
						Summary next = toAdd.next();
						next.pointer = firstPointer;
						nullifyRecordmembers(next);
						volumesHaveBeenMoved.add(next.label);
					}
					
					Iterator<Summary> targetGroup = firstMembers.iterator();
					while (targetGroup.hasNext()) {
						Summary next = targetGroup.next();
						nullifyRecordmembers(next);
						volumesHaveBeenMoved.add(next.label);
					}
					
					firstMembers.addAll(secondMembers);
					secondMembers.clear();
				}
			}
		}
	}
	
	public ArrayList<HashSet<Summary>> sortClustersBySize() {
		Collections.sort(clusters, new Comparator<Set<?>>() {
		    @Override
		    public int compare(Set<?> o1, Set<?> o2) {
		        return Integer.valueOf(o2.size()).compareTo(o1.size());
		    }
		});
		return clusters;
	}
	
	public ArrayList<Cluster> sortClustersByCoherence() {
		ArrayList<Cluster> sortingList = new ArrayList<Cluster>();
		for (HashSet<Summary> thisSet : clusters) {
			Cluster newCluster = new Cluster(thisSet);
			sortingList.add(newCluster);
		}
		Collections.sort(sortingList);
	
		return sortingList;
	}

	
	private boolean okayToMove(HashSet<Summary> thisSet, HashSet<String> volumesHaveBeenMoved) {
		if (thisSet.size() > 1) return true;
		if (thisSet.size() < 1) {
			System.out.println("This should not happen.");
			return false;
		}
		else {
			Iterator<Summary> iter = thisSet.iterator();
			Summary onlyMember = iter.next();
			ArrayList<String> volumesInRecord = onlyMember.volumesInRecord;
			if (volumesInRecord.size() < 2) return true;
			else {
				for (String vol : volumesInRecord) {
					if (volumesHaveBeenMoved.contains(vol)) return false;
				}
				return true;
			}
		}
		
	}
	
	private void nullifyRecordmembers(Summary sum) {
		// If a record object has been clustered, kill the
		// pointers on all its volumes so they can't be
		// individually reassigned to anything.
		if (sum.label.equals(sum.recordID)) {
			ArrayList<String> volsToNullify = sum.volumesInRecord;
			for (Summary nextsum : summaries) {
				for (String volLabel : volsToNullify) {
					if (volLabel.equals(nextsum.label)) {
						nextsum.pointer = -1;
					}
				}
			}
			
		}
	}

}
