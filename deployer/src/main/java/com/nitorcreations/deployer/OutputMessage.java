package com.nitorcreations.deployer;

public class OutputMessage {
	public String name;
	public String line;
	public OutputMessage() {}
	public OutputMessage(String name, String line) {
		this.name = name;
		this.line = line;
	}
	public String toString() {
		return name + ":" + line;
	}
}
