package edu.iastate.metnet.metaomgraph;

import java.util.List;

public class ComputeMean {
	/* 
	 * This function takes the array list, computes and returns the mean of those elements.
	 */
	public double calculateMean(List<Double> list) {
		double sum = 0d;
		for(int i=0; i<list.size(); i++) {
			sum = sum + list.get(i);
		}
		return sum/list.size();
	}

}
