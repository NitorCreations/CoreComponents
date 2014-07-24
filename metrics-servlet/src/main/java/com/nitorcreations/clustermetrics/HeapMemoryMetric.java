package com.nitorcreations.clustermetrics;


public class HeapMemoryMetric extends SimpleMetric {

	@Override
	public String getIndex() {
		return "jmx";
	}

	@Override
	public String[] requiresFields() {
		return new String[] { "heapMemory" };
	}

}
