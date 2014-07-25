package com.nitorcreations.messages;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WebSocketTransmitter {
	private final long flushInterval;
	private final URI uri;
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private final ArrayBlockingQueue<AbstractMessage> queue = new ArrayBlockingQueue<AbstractMessage>(200);
	private final Worker worker = new Worker();
	private final Thread workerThread = new Thread(worker, "websocket-transfer");
	private static final Map<String, WebSocketTransmitter> singletonTransmitters = Collections.synchronizedMap(new HashMap<String, WebSocketTransmitter>());
	private final MessageMapping msgmap = new MessageMapping();
	
	public static WebSocketTransmitter getSingleton(long flushInterval, String uri) throws URISyntaxException {
		WebSocketTransmitter ret = singletonTransmitters.get(uri);
		if (ret == null) {
			ret = new WebSocketTransmitter(flushInterval, uri);
			singletonTransmitters.put(uri, ret);
		}
		return ret;
	}
	
	public WebSocketTransmitter(long flushInterval, String uri) throws URISyntaxException {
		this(flushInterval, new URI(uri));
	}
	
	public WebSocketTransmitter(long flushInterval, URI statUri) {
		this.flushInterval = flushInterval;
		this.uri = statUri;
		logger.info(String.format("Configured to transmit to %s every %d milliseconds", uri.toString(), flushInterval));
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
		logger.fine("Queue message type: " + msgmap.map(msg.getClass()));
		try {
			while (!queue.offer(msg, flushInterval * 2, TimeUnit.MILLISECONDS)) {
				logger.info("queue full, retrying");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@WebSocket
	public class Worker implements Runnable {
		private boolean running=true;
		private final ArrayList<AbstractMessage> send = new ArrayList<AbstractMessage>();
		private Session wsSession;
		@Override
		public void run() {
			synchronized (this) {
				connect();
				while (running) {
					try {
						this.wait(flushInterval);
						if (wsSession == null) continue;
						queue.drainTo(send);
						if (send.size() > 0) {
							logger.fine(String.format("Sending %d messages", send.size()));
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
	            logger.info(String.format("Connecting to : %s", uri));
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
	            logger.info(String.format("Got connect: %s", session));
	            this.wsSession = session;
	            this.notifyAll();
			}
	    }
	 
	    @OnWebSocketClose
	    public void onClose(int statusCode, String reason) {
	    	logger.info(String.format("Connection closed: %d - %s", statusCode, reason));
	        this.wsSession = null;
	        connect();
	    }
	    

	}
}
