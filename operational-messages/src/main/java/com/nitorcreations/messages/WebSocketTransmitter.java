package com.nitorcreations.messages;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WebSocketTransmitter {
	private final long flushInterval;
	private final URI uri;
	private final ArrayBlockingQueue<AbstractMessage> queue = new ArrayBlockingQueue<AbstractMessage>(200);
	private final Worker worker = new Worker();
	private final Thread workerThread = new Thread(worker, "websocket-transfer");
	
	public WebSocketTransmitter(long flushInterval, String uri) throws URISyntaxException {
		this.flushInterval = flushInterval;
		this.uri = new URI(uri);
	}
	
	public WebSocketTransmitter(int flushInterval, URI statUri) {
		this.flushInterval = flushInterval;
		this.uri = statUri;
	}

	public void start() {
		if (!workerThread.isAlive()) {
			workerThread.start();
		}
	}
	public void stop() {
		if (workerThread.isAlive()) {
			worker.stop();
		}
	}
	
	public void queue(AbstractMessage msg) {
		queue.add(msg);
	}
	
	@WebSocket
	public class Worker implements Runnable {
		private boolean running;
		private final ArrayList<AbstractMessage> send = new ArrayList<AbstractMessage>();
		private final MessageMapping msgmap = new MessageMapping();
		private Session wsSession;
		@Override
		public void run() {
			synchronized (this) {
				while (running) {
					try {
						this.wait(flushInterval);
						if (wsSession == null) continue;
						queue.drainTo(send);
						if (send.size() > 0) {
							wsSession.getRemote().sendBytes(msgmap.encode(send));
							send.clear();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		public void stop() {
			synchronized (this) {
				this.running = false;
				this.notifyAll();
			}
		}

		private void connect() {
	        WebSocketClient client = new WebSocketClient();
	        try {
	            client.start();
	            ClientUpgradeRequest request = new ClientUpgradeRequest();
	            client.connect(this, uri, request);
	            System.out.printf("Connecting to : %s%n", uri);
	        } catch (Throwable t) {
	            t.printStackTrace();
	        }
	        synchronized (this) {
	        	while (wsSession == null) {
	        		try {
						this.wait(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
	        	}
	        }
	    }
		@OnWebSocketConnect
	    public void onConnect(Session session) {
	    	synchronized (this) {
	            System.out.printf("Got connect: %s%n", session);
	            this.wsSession = session;
	            this.notifyAll();
			}
	    }
	 
	    @OnWebSocketClose
	    public void onClose(int statusCode, String reason) {
	        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
	        this.wsSession = null;
	        connect();
	    }
	    

	}
}
