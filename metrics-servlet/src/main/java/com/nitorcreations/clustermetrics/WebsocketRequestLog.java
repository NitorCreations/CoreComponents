package com.nitorcreations.clustermetrics;

import java.net.URISyntaxException;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.nitorcreations.messages.AccessLogEntry;
import com.nitorcreations.messages.MessageMapping;
import com.nitorcreations.messages.WebSocketTransmitter;


@ManagedObject("NCSA standard format request log")
public class WebsocketRequestLog extends AbstractLifeCycle implements RequestLog {
	
	private final WebSocketTransmitter transmitter;
    private transient PathMap<String> _ignorePathMap = new PathMap<String>();
	private boolean _preferProxiedForAddress=true;
	private MessageMapping mapping = new MessageMapping();

	public WebsocketRequestLog(long flushInterval, String url) throws URISyntaxException {
		super();
		transmitter = WebSocketTransmitter.getSingleton(flushInterval, url);
		transmitter.start();
	}
	
	@Override
	public void log(Request request, Response response)    {
		AccessLogEntry msg = new AccessLogEntry();
		if (_ignorePathMap != null && _ignorePathMap.getMatch(request.getRequestURI()) != null)
			return;

		String addr = null;
		if (_preferProxiedForAddress)
		{
			addr = request.getHeader(HttpHeader.X_FORWARDED_FOR.toString());
		}

		if (addr == null) {
			addr = request.getRemoteAddr();
		}
		msg.setRemoteAddr(addr);

		Authentication authentication = request.getAuthentication();
		if (authentication instanceof Authentication.User) {
			msg.setAuthentication(((Authentication.User)authentication).getUserIdentity().getUserPrincipal().getName());
		}


		msg.timestamp = request.getTimeStamp();
		msg.setMethod(request.getMethod());
		msg.setUri(request.getUri().toString());
		msg.setProtocol(request.getProtocol());

		int status = response.getStatus();
		if (status <= 0)
			status = 404;
		msg.setStatus(status);
		msg.setResponseLength(response.getLongContentLength());
		long now = System.currentTimeMillis();
		msg.setDuration(now - request.getTimeStamp());
		msg.setReferrer(request.getHeader(HttpHeader.REFERER.toString()));
		msg.setAgent(request.getHeader(HttpHeader.USER_AGENT.toString()));
		transmitter.queue(msg);
	}

	public void setPreferProxiedForAddress(boolean b) {
		this._preferProxiedForAddress = b;
	}
}
