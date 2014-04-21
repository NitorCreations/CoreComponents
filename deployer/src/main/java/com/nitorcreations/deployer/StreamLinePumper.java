package com.nitorcreations.deployer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

@WebSocket
class StreamLinePumper implements Runnable {
	private Session session;
	private final CountDownLatch closeLatch = new CountDownLatch(1);
	private URI statUri;
	private final InputStream in;
	private final String name;

	public StreamLinePumper(InputStream in, Session session, String name) throws URISyntaxException {
		this.session = session;
		this.in = in;
		this.name = name;
	}

	@Override
	public void run() {
		try {
			byte[] buffer = new byte[1024];
			int len;
			while (true) {
				if ((len = in.read(buffer)) != -1) {
					System.out.printf("Read %d bytes\n", len);
					if (session != null) {
						session.getRemote().sendString(name + ": " + new String(buffer, 0, len));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.printf("Got connect: %s%n", session);
        this.session = session;
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown();
    }

}