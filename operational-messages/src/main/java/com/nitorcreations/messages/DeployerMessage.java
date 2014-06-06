package com.nitorcreations.messages;

public class DeployerMessage {
	public int type;
	public byte[] message;
	public DeployerMessage() {}
	public DeployerMessage(int type, byte[] message) {
		this.type = type;
		this.message = message;
	}
}
