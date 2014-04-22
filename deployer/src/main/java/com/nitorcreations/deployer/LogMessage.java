package com.nitorcreations.deployer;

public class LogMessage {
	public long timeStamp;
	public String level;
	public String message;
	public LogMessage() {}
	public LogMessage(long timeStamp, String level, String message) {
		this.timeStamp = timeStamp;
		this.level = level;
		this.message = message;
	}
}
