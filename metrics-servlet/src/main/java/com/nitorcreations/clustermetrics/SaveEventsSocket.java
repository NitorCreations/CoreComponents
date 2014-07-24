package com.nitorcreations.clustermetrics;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;

import com.google.gson.Gson;
import com.nitorcreations.messages.AbstractMessage;
import com.nitorcreations.messages.HashMessage;
import com.nitorcreations.messages.LongStatisticsMessage;
import com.nitorcreations.messages.MessageMapping;
import com.nitorcreations.messages.MessageMapping.MessageType;

@WebSocket
public class SaveEventsSocket {
    private final CountDownLatch closeLatch;
    private final MessageMapping mapping = new MessageMapping();
    private final Client client = MetricsServlet.getClient();
    
    @SuppressWarnings("unused")
    private Session session;
    private String path;
    
    public SaveEventsSocket() {
        this.closeLatch = new CountDownLatch(1);
    }
 
    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }
 
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown();
    }
 
    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.printf("Got connect: %s%n", session);
        this.session = session;
        path = session.getUpgradeRequest().getRequestURI().getPath().substring("/statistics/".length());
    }
     
    @OnWebSocketMessage
    public void messageReceived(byte buf[], int offset, int length) {
    	try {
    		for (AbstractMessage msgObject : mapping.decode(buf, offset, length)) {
    			MessageType type = mapping.map(msgObject.getClass());
    			Object stored = msgObject;
    			if (type == MessageType.LONGSTATS) {
    				stored = ((LongStatisticsMessage)msgObject).getMap();
    			} else if (type == MessageType.HASH) {
    				stored = ((HashMessage)msgObject).getMap();
    			}
    			String source = "{ \"instance\" : \"" + path + "\", " + new Gson().toJson(stored).substring(1);
    			System.out.println(source);
    			IndexResponse resp = client.prepareIndex(type.lcName(), type.lcName())
    					.setSource(source)
    					.execute()
    					.actionGet(1000);
    			if (!resp.isCreated()) {
    				System.out.println("Failed to create index for " + source);
    			}
    		}
    	} catch (Throwable e) {
    		e.printStackTrace();
    	}
    }
}