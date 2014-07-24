package com.nitorcreations.clustermetrics;

import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@WebServlet(name = "Statistics WebSocket Servlet", urlPatterns = { "/statistics" })
public class StatisticsServlet extends WebSocketServlet {
	private static final long serialVersionUID = -7940037116569261919L;

	@Override
	public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(TimeUnit.HOURS.toMillis(10));
        factory.register(SaveEventsSocket.class);
	}

}
