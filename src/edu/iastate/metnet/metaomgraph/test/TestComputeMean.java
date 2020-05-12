package edu.iastate.metnet.metaomgraph.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.runners.Parameterized.Parameters;

import edu.iastate.metnet.metaomgraph.ComputeMean;

public class TestComputeMean {
	@Parameters
	public static List<Double> data() {
		List<Double> list = new ArrayList<Double>();
		list.add(0.1d);
		list.add(0.2d);
		list.add(0.3d);
		return list;
	}
	
	@Test
	public void testCalculateMean() {
		ComputeMean cm = new ComputeMean();
		assertEquals(0.2d, cm.calculateMean(data()));
	}
}
