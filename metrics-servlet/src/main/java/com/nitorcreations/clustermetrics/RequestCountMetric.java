package com.nitorcreations.clustermetrics;

import java.util.List;


public class RequestCountMetric extends SimpleMetric {

	@Override
	public String getIndex() {
		return "access";
	}

	@Override
	public String[] requiresFields() {
		return new String[] { "duration" };
	}
	
	@Override
	protected Number estimateValue(List<Number> preceeding, Long stepTime) {
		return preceeding.size();
	}

}
