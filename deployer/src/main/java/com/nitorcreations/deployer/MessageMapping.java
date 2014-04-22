package com.nitorcreations.deployer;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.ProcStat;
import org.msgpack.MessagePack;

public class MessageMapping {
	public enum MessageType {
		PROC, CPU, MEM, DISK, OUTPUT, LOG; 
	}
	private Map<MessageType, Class<?>> messageTypes = new HashMap<MessageMapping.MessageType, Class<?>>();

	public MessageMapping() {
		messageTypes.put(MessageType.PROC, ProcStat.class);
		messageTypes.put(MessageType.CPU, Cpu.class);
		messageTypes.put(MessageType.MEM, Mem.class);
		messageTypes.put(MessageType.DISK, FileSystemUsage.class);
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
