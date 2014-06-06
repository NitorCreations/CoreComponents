package com.nitorcreations.messages;

import java.util.LinkedHashMap;
import java.util.Map;

import org.msgpack.annotation.Message;

@Message
public class AccessLogEntry extends AbstractMessage {
	String remoteAddr;
	String authentication;
	private String method;
	private String uri;
	private String protocol;
	private int status;
	private long responseLength;
	private long duration;
	private String referrer;
	private String agent;

	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getMethod() {
		return method;
	}
	public String getProtocol() {
		return protocol;
	}
	public int getStatus() {
		return status;
	}
	public long getResponseLength() {
		return responseLength;
	}
	public long getDuration() {
		return duration;
	}
	public String getReferrer() {
		return referrer;
	}
	public String getAgent() {
		return agent;
	}
	public void setRemoteAddr(String addr) {
		this.remoteAddr = addr;
	}
	public String getRemoteAddr() {
		return remoteAddr;
	}
	public void setAuthentication(String name) {
		this.authentication = name;
	}
	public String getAuthentication() {
		return authentication;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public void setResponseLength(long contentLength) {
		this.responseLength = contentLength;
	}
	public void setDuration(long l) {
		this.duration = l;
	}
	public void setReferrer(String referer) {
		this.referrer = referer;
	}
	public void setAgent(String agent) {
		this.agent = agent;
	}
	
}
