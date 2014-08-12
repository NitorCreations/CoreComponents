package com.nitorcreations.clustermetrics;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.facet.histogram.HistogramFacet.Entry;

public class PropertiesServlet implements Servlet {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public ServletConfig getServletConfig() {
		return null;
	}

	@Override
	public String getServletInfo() {
		return null;
	}

	@Override
	public void init(ServletConfig conf) throws ServletException {

	}

	@Override
	public void service(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		if (!((HttpServletRequest)request).getMethod().equals("GET")) {
			((HttpServletResponse)response).sendError(405, "Only GET allowed");
			return;
		}
		String rootProps = ((HttpServletRequest)request).getPathInfo();
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();
		VelocityContext context = new VelocityContext();
		for (java.util.Map.Entry<String, String[]> next: request.getParameterMap().entrySet()) {
			context.put(next.getKey(), String.join(",", next.getValue()));
		}
		StringWriter out = new StringWriter();
		Template t = ve.getTemplate(rootProps);
		t.merge(context, out);
		Properties p = new Properties();
		p.load(new StringReader(out.toString()));
		
		
	}

}
