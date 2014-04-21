package com.nitorcreations.deployer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

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

@WebSocket
public class StatsSender implements Runnable {
	private AtomicBoolean running = new AtomicBoolean(true);
	private Session session;
	
	public StatsSender(Session session) throws URISyntaxException {
		this.session = session;
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
		FileSystemUsage[] dStat;
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
							session.getRemote().sendString(pStat.toString());
						} catch (SigarException e) {
							e.printStackTrace();
							return;
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
							session.getRemote().sendString(cStat.toString());
						} catch (SigarException e) {
							e.printStackTrace();
							return;
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
							session.getRemote().sendString(mem.toString());
						} catch (SigarException e) {
							e.printStackTrace();
							return;
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
							dStat = new FileSystemUsage[fileSystems.length];
							for (int i=0; i < fileSystems.length; i++) {
								dStat[i] = sigar.getMountedFileSystemUsage(fileSystems[i].getDirName());
							}
							for (FileSystemUsage next : dStat) {
								session.getRemote().sendString(next.toString());
							}
						} catch (SigarException e) {
							e.printStackTrace();
							return;
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
