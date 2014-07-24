package com.nitorcreations.clustermetrics;

import org.elasticsearch.action.search.SearchResponse;

public interface HistogramMetric extends Metric {
	Object calculateHistogram(SearchResponse response, double[] buckets, long start, long stop, int step);
}