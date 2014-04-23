package com.nitorcreations.deployer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.harmony.beans.BeansUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.ProcStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.msgpack.MessagePack;

import com.nitorcreations.deployer.messages.CPU;
import com.nitorcreations.deployer.messages.DiskUsage;
import com.nitorcreations.deployer.messages.Memory;
import com.nitorcreations.deployer.messages.MessageMapping;
import com.nitorcreations.deployer.messages.Processes;
import com.nitorcreations.deployer.messages.MessageMapping.MessageType;

@WebSocket
public class StatsSender implements Runnable {
	private AtomicBoolean running = new AtomicBoolean(true);
	private Session session;
	private MessagePack msgpack = new MessagePack();
	
	public StatsSender(Session session) throws URISyntaxException {
		this.session = session;
		new MessageMapping().registerTypes(msgpack);
	}

	public void stop() {
		running.set(false);
		this.notifyAll();
	}

	@Override
	public void run() {
		Sigar sigar = new Sigar();

		long nextProcs = System.currentTimeMillis() + 5000;
		long nextCpus =  System.currentTimeMillis() + 5000;
		long nextMem =  System.currentTimeMillis() + 5000;
		long nextDisks = System.currentTimeMillis() + 60000;
		ProcStat pStat;
		DiskUsage[] dStat;
		Cpu cStat;
		Mem mem;
		while (running.get()) {
			long now = System.currentTimeMillis();
			FileSystem[] fileSystems;
			if (now > nextProcs) {
				try {
					if (session != null)
						try {
							pStat = sigar.getProcStat();
							Processes msg = new Processes();
							PropertyUtils.copyProperties(msg, pStat);
							byte[] message = msgpack.write(msg);
							session.getRemote().sendBytes(ByteBuffer.wrap(msgpack.write(new DeployerMessage(MessageType.PROC.ordinal(), message))));
						} catch (SigarException e) {
							e.printStackTrace();
							return;
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						}
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextProcs = nextProcs + 5000;
			}
			if (now > nextCpus) {
				try {
					if (session != null)
						try {
							cStat = sigar.getCpu();
							CPU msg = new CPU();
							PropertyUtils.copyProperties(msg, cStat);
							byte[] message = msgpack.write(msg);
							session.getRemote().sendBytes(ByteBuffer.wrap(msgpack.write(new DeployerMessage(MessageType.CPU.ordinal(), message))));
						} catch (SigarException e) {
							e.printStackTrace();
							return;
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						}
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextCpus = nextCpus + 5000;
			}
			if (now > nextMem) {
				try {
					if (session != null)
						try {
							mem = sigar.getMem();
							Memory msg = new Memory();
							PropertyUtils.copyProperties(msg, mem);
							byte[] message = msgpack.write(msg);
							session.getRemote().sendBytes(ByteBuffer.wrap(msgpack.write(new DeployerMessage(MessageType.MEM.ordinal(), message))));
						} catch (SigarException e) {
							e.printStackTrace();
							return;
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						}
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextMem = nextMem + 5000;
			}
			if (now > nextDisks) {
				try {
					if (session != null)
						try {
							fileSystems = sigar.getFileSystemList();
							dStat = new DiskUsage[fileSystems.length];
							for (int i=0; i < fileSystems.length; i++) {
								FileSystemUsage next = sigar.getMountedFileSystemUsage(fileSystems[i].getDirName());
								dStat[i] = new DiskUsage();
								PropertyUtils.copyProperties(dStat[i], next);
								dStat[i].setName(fileSystems[i].getDirName());
							}
							for (DiskUsage next : dStat) {
								byte[] message = msgpack.write(next);
								session.getRemote().sendBytes(ByteBuffer.wrap(msgpack.write(new DeployerMessage(MessageType.DISK.ordinal(), message))));
							}
						} catch (SigarException e) {
							e.printStackTrace();
							return;
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						}
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextDisks = nextDisks + 60000;
			}
		}
		session.close(StatusCode.NORMAL, "I'm done");
	}
	    
}
