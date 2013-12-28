package deduplication;
import java.util.*;

/*
 * Jaccard Similarity is a similarity function which is calculated by 
 * first tokenizing the strings into sets and then taking the ratio of
 * (weighted) intersection to their union.
 * FROM:
 *  www.cs.rit.edu/~vvs1100/Code/JaccardSimilarity.java
 */
public class JaccardSimilarity {

	public static double jaccardSimilarity(String similar1, String similar2){
		HashSet<String> h1 = new HashSet<String>();
		HashSet<String> h2 = new HashSet<String>();
		
		for(String s: similar1.split("\\s+")){
			if (s.equals("the") | s.equals("a") | s.equals("<blank>")) continue;
			else h1.add(s.replaceAll("[^a-zA-Z]", ""));		
		}
		
		// The effect of these is to split the string around all whitespace characters,
		// and then zap all nonalphabetic characters in the parts that remain.
		
		for(String s: similar2.split("\\s+")){
			if (s.equals("the") | s.equals("a") | s.equals("<blank>")) continue;
			else h2.add(s.replaceAll("[^a-zA-Z]", ""));		
		}
		
		
		int sizeh1 = h1.size();
		//Retains all elements in h3 that are contained in h2 ie intersection
		h1.retainAll(h2);
		//h1 now contains the intersection of h1 and h2
		
			
		h2.removeAll(h1);
		//h2 now contains unique elements
		
		//Union 
		int union = sizeh1 + h2.size();
		int intersection = h1.size();
		
		if (union == 0) return 0;
		else return (double)intersection/union;
		
	}
	public static void main(String args[]){
		System.out.println(jaccardSimilarity("153 West Squire Dr","147 West Squire Dr"));
		
	}
}
