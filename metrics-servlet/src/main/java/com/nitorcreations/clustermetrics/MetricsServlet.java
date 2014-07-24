package com.nitorcreations.clustermetrics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.gson.Gson;

public class MetricsServlet implements Servlet {
	private static Node node;
	
	Map<String, Metric> metrics = new HashMap<String, Metric>();
	ServletConfig config;

	private Settings settings;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		this.config = config;
		metrics.put("/heap", new HeapMemoryMetric());
		metrics.put("/mem", new PhysicalMemoryMetric());
		metrics.put("/requests", new RequestCountMetric());
		metrics.put("/latency", new RequestDurationMetric());
		setupElasticSearch(config.getServletContext());
        node = NodeBuilder.nodeBuilder()
        		.settings(settings)
        		.clusterName("liipy-deploy")
        		.data(true).local(true).node();

	}

	@Override
	public ServletConfig getServletConfig() {
		return config;
	}

	@Override
	public void service(ServletRequest req, ServletResponse res)
			throws ServletException, IOException {
		if (!((HttpServletRequest)req).getMethod().equals("GET")) {
			((HttpServletResponse)res).sendError(405, "Only GET allowed");
			return;
		}
		Client client = getClient();
		String metricKey = ((HttpServletRequest)req).getPathInfo();
		Metric metric = metrics.get(metricKey);
		if (metric == null) {
			((HttpServletResponse)res).sendError(404, "Metric " + metricKey + " not found");
			return;
		}
		long start = Long.parseLong(req.getParameter("start"));
		long stop = Long.parseLong(req.getParameter("stop"));
		int step = Integer.parseInt(req.getParameter("step"));
		double[] buckets = null;
		if  (metric instanceof HistogramMetric) {
			String[] bucketsStr = req.getParameter("buckets").split(",");
			buckets = new double[bucketsStr.length];
			int i=0;
			for (String next : bucketsStr) {
				buckets[i++] = Double.parseDouble(next);
			}
		}
		
		SearchResponse response = client.prepareSearch(metric.getIndex(), metric.getIndex())
				.setQuery(QueryBuilders.rangeQuery("timestamp")
						.from(start - step)
						.to(stop + step)
						.includeLower(false)
						.includeUpper(true))
						.setSearchType(SearchType.QUERY_AND_FETCH)
						.setSize(5000)
						.addField("timestamp")
						.addFields(metric.requiresFields()).get();
		Object data=null;
		if (metric instanceof HistogramMetric) {
			data = ((HistogramMetric) metric).calculateHistogram(response, buckets, start, stop, step);
		} else {
			data = metric.calculateMetric(response, start, stop, step);
		}
		res.setContentType("application/json");
		Gson out = new Gson();
		res.getOutputStream().write(out.toJson(data).getBytes());
	}

	@Override
	public String getServletInfo() {
		return "Cluster metrics";
	}

	@Override
	public void destroy() {

	}

	public static Client getClient() {
    	return node.client();
    }

    private void setupElasticSearch(ServletContext context) {
        ImmutableSettings.Builder settingsBuilder = ImmutableSettings.settingsBuilder();
        settingsBuilder.put("node.name", context.getInitParameter("node.name"));
        settingsBuilder.put("path.data", "data/index");
        settingsBuilder.put("http.enabled", true);
        settingsBuilder.put("http.port", 5240);
        this.settings = settingsBuilder.build();
	}


}
