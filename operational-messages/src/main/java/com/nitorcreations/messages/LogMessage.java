package com.nitorcreations.messages;

import org.msgpack.annotation.Message;

@Message
public class LogMessage extends AbstractMessage {
	public long logEntryTimeStamp;
	public String level;
	public String message;
	public String stackTrace;
	public LogMessage() {}
	
	public LogMessage(long logEntryTimeStamp, String level, String message) {
		this.logEntryTimeStamp = logEntryTimeStamp;
		this.level = level;
		this.message = message;
		this.stackTrace = "";
	}
	public LogMessage(long logEntryTimeStamp, String level, String message, String stackTrace) {
		this.logEntryTimeStamp = logEntryTimeStamp;
		this.level = level;
		this.message = message;
		this.stackTrace = stackTrace;
	}
	public long getLogEntryTimeStamp() {
		return logEntryTimeStamp;
	}
	public void setLogEntryTimeStamp(long logEntryTimeStamp) {
		this.logEntryTimeStamp = logEntryTimeStamp;
	}
	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
