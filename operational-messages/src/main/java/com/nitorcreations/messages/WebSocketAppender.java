package com.nitorcreations.messages;

import java.net.URISyntaxException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class WebSocketAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
	
	private String uri;
	private long flushInterval = 2000;
	private WebSocketTransmitter transmitter;
	
	@Override
	protected void append(ILoggingEvent event) {
        if (event == null || !isStarted())
            return;
        transmitter.queue(new LogMessage(event));
	}
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public long getFlushInterval() {
		return flushInterval;
	}
	public void setFlushInterval(long flush_interval) {
		this.flushInterval = flush_interval;
	}
	
	@Override
	public void start() {
        if (isStarted()) return;
        
        if (uri == null) {
            addError("No remote uri configured for appender " + name);
            return;
        }
        
        try {
			this.transmitter = WebSocketTransmitter.getSingleton(flushInterval, uri);
            super.start();
		} catch (URISyntaxException e) {
			addError("Invalid uri for appender " + name, e);
		}

	}
}
