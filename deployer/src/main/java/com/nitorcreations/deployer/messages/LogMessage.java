package com.nitorcreations.deployer.messages;

import org.msgpack.annotation.Message;

@Message
public class LogMessage extends AbstractMessage {
	public long timeStamp;
	public String level;
	public String message;
	public LogMessage() {}
	public LogMessage(long timeStamp, String level, String message) {
		this.timeStamp = timeStamp;
		this.level = level;
		this.message = message;
	}
	public long getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
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
