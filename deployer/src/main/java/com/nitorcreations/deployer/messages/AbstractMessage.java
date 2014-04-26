package com.nitorcreations.deployer.messages;

import com.google.gson.Gson;

public class AbstractMessage {
	public long timestamp = System.currentTimeMillis();
	public String toString() {
		return new Gson().toJson(this);
	}
}
