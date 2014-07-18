package com.nitorcreations.deployer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;


import com.nitorcreations.messages.OutputMessage;
import com.nitorcreations.messages.WebSocketTransmitter;

class StreamLinePumper implements Runnable {
	private final BufferedReader in;
	private final String name;
	private final WebSocketTransmitter transmitter;

	public StreamLinePumper(InputStream in, WebSocketTransmitter transmitter, String name) throws URISyntaxException {
		this.transmitter = transmitter;
		this.in = new BufferedReader(new InputStreamReader(in));
		this.name = name;
	}
	
	@Override
	public void run() {
		try {
			String line;
			while ((line = in.readLine()) != null) {
				OutputMessage msg = new OutputMessage(name, line);
				transmitter.queue(msg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}