package com.nitorcreations.clustermetrics;

import java.util.List;


public class RequestDurationMetric extends SimpleMetric {

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
		long sum = 0;
		for (Number next : preceeding) {
			sum += next.longValue();
		}
		return sum / preceeding.size();
	}

}
