package com.nitorcreations.clustermetrics;


public class PhysicalMemoryMetric extends SimpleMetric {

	@Override
	public String getIndex() {
		return "mem";
	}

	@Override
	public String[] requiresFields() {
		return new String[] { "usedPercent" };
	}

}
