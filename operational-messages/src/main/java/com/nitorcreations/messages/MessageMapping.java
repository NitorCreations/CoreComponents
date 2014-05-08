package com.nitorcreations.messages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

import org.msgpack.MessagePack;

public class MessageMapping {
	MessagePack msgpack = new MessagePack();

	public enum MessageType {
		PROC, CPU, MEM, DISK, OUTPUT, LOG, JMX, PROCESSCPU, ACCESS;
		public String lcName() {
			return toString().toLowerCase();
		}
	}

	private Map<MessageType, Class<? extends AbstractMessage>> messageTypes = new HashMap<MessageMapping.MessageType, Class<? extends AbstractMessage>>();
	private Map<Class<? extends AbstractMessage>, MessageType> messageClasses = new HashMap<Class<? extends AbstractMessage>, MessageType>();

	public MessageMapping() {
		messageTypes.put(MessageType.PROC, Processes.class);
		messageTypes.put(MessageType.CPU, CPU.class);
		messageTypes.put(MessageType.MEM, Memory.class);
		messageTypes.put(MessageType.DISK, DiskUsage.class);
		messageTypes.put(MessageType.OUTPUT, OutputMessage.class);
		messageTypes.put(MessageType.LOG, LogMessage.class);
		messageTypes.put(MessageType.JMX, JmxMessage.class);
		messageTypes.put(MessageType.PROCESSCPU, ProcessCPU.class);
		messageTypes.put(MessageType.ACCESS, AccessLogEntry.class);

		for (java.util.Map.Entry<MessageType, Class<? extends AbstractMessage>> next : messageTypes.entrySet()) {
			messageClasses.put(next.getValue(), next.getKey());
		}
		registerTypes(msgpack);
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
	public MessageType map(Class<?> msgclass) {
		return messageClasses.get(msgclass);
	}

	public ByteBuffer encode(Object msg) throws IOException {
		LZ4Factory factory = LZ4Factory.fastestInstance();
		LZ4Compressor compressor = factory.fastCompressor();
		byte[] message = msgpack.write(msg);
		MessageType type = map(msg.getClass()); 
		byte[] data = msgpack.write(new DeployerMessage(type.ordinal(), message));
		int maxCompressedLength = compressor.maxCompressedLength(data.length);
		byte[] compressed = new byte[maxCompressedLength];
		int compressedLength = compressor.compress(data, 0, data.length, compressed, 0, maxCompressedLength);
		return ByteBuffer.wrap(compressed, 0, compressedLength);
	}

	public Object decode(byte[] data, int offset, int length) throws IOException {
		LZ4Factory factory = LZ4Factory.fastestInstance();
		LZ4SafeDecompressor decompressor = factory.safeDecompressor();
		byte[] restored = decompressor.decompress(data, offset, length, length * 5);
		DeployerMessage msg = msgpack.read(restored, DeployerMessage.class);
		return msgpack.read(msg.message, map(msg.type));
	}
}
