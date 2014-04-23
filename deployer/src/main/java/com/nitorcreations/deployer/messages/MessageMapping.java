package com.nitorcreations.deployer.messages;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.FileSystemUsage;
import org.msgpack.MessagePack;

import com.nitorcreations.deployer.DeployerMessage;

public class MessageMapping {
	
	public enum MessageType {
		PROC, CPU, MEM, DISK, OUTPUT, LOG; 
	}
	private Map<MessageType, Class<?>> messageTypes = new HashMap<MessageMapping.MessageType, Class<?>>();

	public MessageMapping() {
		messageTypes.put(MessageType.PROC, Processes.class);
		messageTypes.put(MessageType.CPU, CPU.class);
		messageTypes.put(MessageType.MEM, Memory.class);
		messageTypes.put(MessageType.DISK, DiskUsage.class);
		messageTypes.put(MessageType.OUTPUT, OutputMessage.class);
		messageTypes.put(MessageType.LOG, LogMessage.class);
	}

	public void registerTypes(MessagePack msgpack) {
		for (Class<?> next: messageTypes.values()) {
			msgpack.register(next );
		}
		msgpack.register(DeployerMessage.class);
	}

	public Class<?> map(int type) {
		return messageTypes.get(MessageType.values()[type]);
	}
	
	
}
