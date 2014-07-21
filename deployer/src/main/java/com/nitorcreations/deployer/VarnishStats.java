package com.nitorcreations.deployer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nitorcreations.messages.LongStatisticsMessage;
import com.nitorcreations.messages.WebSocketTransmitter;

public class VarnishStats implements Runnable {
	private final WebSocketTransmitter transmitter;
	private final long interval;
	private boolean running=true;

	public VarnishStats(WebSocketTransmitter transmitter, long interval) {
		this.transmitter = transmitter;
		this.interval = interval;
	}

	public void run() {
		synchronized (this) {
			while (running) {
				try {
					this.wait(interval);
					send();
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void stop() {
		synchronized (this) {
			running = false;
			this.notifyAll();
		}
	}

	public void send() throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder("/usr/bin/varnishstat", "-j");
		pb.environment().putAll(System.getenv());
		Process p = pb.start();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		StreamPumper stdout = new StreamPumper(p.getInputStream(), out);
		StreamPumper stderr = new StreamPumper(p.getErrorStream(), err);
		new Thread(stdout, "stdout").start();
		new Thread(stderr, "stderr").start();
		if (p.waitFor() == 0) {
			Map<String, Long> values = new LinkedHashMap<String, Long>();
			JsonObject j = new JsonParser().parse(new String(out.toByteArray())).getAsJsonObject();
			for (Entry<String,JsonElement> entry : j.entrySet()) {
				if (entry.getValue().isJsonObject()) {
					values.put(entry.getKey(), entry.getValue().getAsJsonObject().get("value").getAsLong());
				}
			}
			LongStatisticsMessage send = new LongStatisticsMessage();
			send.setMap(values);
			transmitter.queue(send);
		} else {
			System.err.write(err.toByteArray());
		}
	}
}
