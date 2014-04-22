package com.nitorcreations.deployer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.msgpack.MessagePack;

import com.nitorcreations.deployer.MessageMapping.MessageType;

@WebSocket
class StreamLinePumper implements Runnable {
	private Session session;
	private final CountDownLatch closeLatch = new CountDownLatch(1);
	private final BufferedReader in;
	private final String name;
	private MessagePack msgpack = new MessagePack();

	public StreamLinePumper(InputStream in, Session session, String name) throws URISyntaxException {
		this.session = session;
		this.in = new BufferedReader(new InputStreamReader(in));
		this.name = name;
		msgpack.register(OutputMessage.class);
		msgpack.register(DeployerMessage.class);
	}
	
	@Override
	public void run() {
		try {
			String line;
			while ((line = in.readLine()) != null) {
				byte[] message = msgpack.write(new OutputMessage(name, line));
				session.getRemote().sendBytes(ByteBuffer.wrap(msgpack.write(new DeployerMessage(MessageType.OUTPUT.ordinal(), message))));
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