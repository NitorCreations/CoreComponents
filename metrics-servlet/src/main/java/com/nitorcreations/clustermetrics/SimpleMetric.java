package com.nitorcreations.clustermetrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

public abstract class SimpleMetric implements Metric {
	protected SortedMap<Long, Number> rawData;
	protected void readResponse(SearchResponse response) {
		rawData = new TreeMap<Long, Number>();
		for (SearchHit next : response.getHits().getHits()) {
			Map<String, SearchHitField> fields = next.getFields();
			Long timestamp = fields.get("timestamp").value();
			List<Number> nextData = new ArrayList<>();
			for (String nextField : requiresFields()) {
				nextData.add((Number)fields.get(nextField).value());
			}
			rawData.put(timestamp, getValue(nextData));
		}
	}
	protected Number getValue(List<Number> arr) {
		return arr.get(0);
	}
	
	@Override
	public Object calculateMetric(SearchResponse response,  long start, long stop, int step) {
		readResponse(response);
		if (rawData.isEmpty()) return new double[0];
		int len = (int)((stop - start)/step) + 1;
		List<TimePoint> ret = new ArrayList<TimePoint>();
		List<Long> retTimes = new ArrayList<Long>();
		long curr=start;
		for (int i=0;i<len;i++) {
			retTimes.add(Long.valueOf(curr));
			curr += step;
		}
		Collection<Number> preceeding = new ArrayList<Number>();
		for (Long nextTime : retTimes) {
			long afterNextTime = nextTime + 1;
			preceeding = rawData.headMap(afterNextTime).values();
			rawData = rawData.tailMap(afterNextTime);
			List<Number> tmplist = new ArrayList<Number>(preceeding);
			if (tmplist.isEmpty()) {
				ret.add(new TimePoint(nextTime.longValue(), fillMissingValue()));
				continue;
			}
			ret.add(new TimePoint(nextTime.longValue(), estimateValue(tmplist, nextTime)));
		}
		return ret;
	}
	
	protected Number estimateValue(List<Number> preceeding, Long stepTime) {
		return preceeding.get(preceeding.size() - 1);
	}

	protected Number fillMissingValue() {
		return 0;
	}
	
}
