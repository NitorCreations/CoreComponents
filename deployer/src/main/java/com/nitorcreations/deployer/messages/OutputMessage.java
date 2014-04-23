package com.nitorcreations.deployer.messages;

import org.msgpack.annotation.Message;

@Message
public class OutputMessage extends AbstractMessage{
	public String name;
	public String line;
	public OutputMessage() {}
	public OutputMessage(String name, String line) {
		this.name = name;
		this.line = line;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLine() {
		return line;
	}
	public void setLine(String line) {
		this.line = line;
	}
}
