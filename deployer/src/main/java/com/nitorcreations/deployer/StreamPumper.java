package com.nitorcreations.deployer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class StreamPumper implements Runnable {
	private final InputStream in;
	private final OutputStream out;

	public StreamPumper(InputStream in, OutputStream out) {
		this.out = out;
		this.in = in;
	}

	@Override
	public void run() {
		byte[] buffer = new byte[4 * 1024];
		try {
			int read;
			while ((read = in.read(buffer)) >= 0) {
				out.write(buffer, 0, read);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}