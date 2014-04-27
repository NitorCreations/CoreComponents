package com.nitorcreations.deployer.messages;

import java.util.HashMap;
import java.util.Map;

import org.msgpack.MessagePack;

import com.nitorcreations.deployer.DeployerMessage;

public class MessageMapping {

	public enum MessageType {
		PROC, CPU, MEM, DISK, OUTPUT, LOG, JMX, PROCESSCPU;
		public String lcName() {
			return toString().toLowerCase();
		}
	}

	private Map<MessageType, Class<? extends AbstractMessage>> messageTypes = new HashMap<MessageMapping.MessageType, Class<? extends AbstractMessage>>();

	public MessageMapping() {
		messageTypes.put(MessageType.PROC, Processes.class);
		messageTypes.put(MessageType.CPU, CPU.class);
		messageTypes.put(MessageType.MEM, Memory.class);
		messageTypes.put(MessageType.DISK, DiskUsage.class);
		messageTypes.put(MessageType.OUTPUT, OutputMessage.class);
		messageTypes.put(MessageType.LOG, LogMessage.class);
		messageTypes.put(MessageType.JMX, JmxMessage.class);
		messageTypes.put(MessageType.PROCESSCPU, ProcessCPU.class);
	}

	public void registerTypes(MessagePack msgpack) {
		msgpack.register(Thread.State.class);
		msgpack.register(ThreadInfoMessage.class);
		msgpack.register(GcInfo.class);
		for (Class<?> next: messageTypes.values()) {
			msgpack.register(next );
		}
		msgpack.register(DeployerMessage.class);
	}

	public Class<? extends AbstractMessage> map(int type) {
		return messageTypes.get(MessageType.values()[type]);
	}
	
	
}
