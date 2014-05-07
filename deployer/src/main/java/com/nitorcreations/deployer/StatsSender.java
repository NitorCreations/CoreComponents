package com.nitorcreations.deployer;

import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;

import org.apache.commons.beanutils.PropertyUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.msgpack.MessagePack;

import com.nitorcreations.messages.CPU;
import com.nitorcreations.messages.DeployerMessage;
import com.nitorcreations.messages.DiskUsage;
import com.nitorcreations.messages.GcInfo;
import com.nitorcreations.messages.JmxMessage;
import com.nitorcreations.messages.Memory;
import com.nitorcreations.messages.MessageMapping;
import com.nitorcreations.messages.ProcessCPU;
import com.nitorcreations.messages.Processes;
import com.nitorcreations.messages.ThreadInfoMessage;
import com.nitorcreations.messages.MessageMapping.MessageType;

@WebSocket
public class StatsSender implements Runnable {
	private AtomicBoolean running = new AtomicBoolean(true);
	private Session session;
	private MessagePack msgpack = new MessagePack();
	private MBeanServerConnection mBeanServerConnection;
	private int pid;
	
	public StatsSender(Session session, MBeanServerConnection mBeanServerConnection, int pid) throws URISyntaxException {
		this.session = session;
		this.mBeanServerConnection = mBeanServerConnection;
		this.pid = pid;
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
		long nextProcCpus =  System.currentTimeMillis() + 5000;
		long nextMem =  System.currentTimeMillis() + 5000;
		long nextJmx = System.currentTimeMillis() + 5000;
		long nextDisks = System.currentTimeMillis() + 60000;
		ProcStat pStat;
		DiskUsage[] dStat;
		Cpu cStat;
		ProcCpu pCStat;
		Mem mem;
		while (running.get()) {
			long now = System.currentTimeMillis();
			FileSystem[] fileSystems;
			if (now > nextProcs) {
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
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextProcs = nextProcs + 5000;
			}
			if (now > nextCpus) {
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
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextCpus = nextCpus + 5000;
			}
			if (now > nextProcCpus) {
				try {
					pCStat = sigar.getProcCpu(pid);
					ProcessCPU  msg = new ProcessCPU();
					PropertyUtils.copyProperties(msg, pCStat);
					byte[] message = msgpack.write(msg);
					session.getRemote().sendBytes(ByteBuffer.wrap(msgpack.write(new DeployerMessage(MessageType.PROCESSCPU.ordinal(), message))));
				} catch (SigarException e) {
					e.printStackTrace();
					return;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextProcCpus = nextProcCpus + 5000;
			}
			if (now > nextMem) {
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
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextMem = nextMem + 5000;
			}
			if (now > nextJmx) {
				try {
					JmxMessage msg = getJmxStats();
					if (msg != null) {
						byte[] message = msgpack.write(msg);
						session.getRemote().sendBytes(ByteBuffer.wrap(msgpack.write(new DeployerMessage(MessageType.JMX.ordinal(), message))));
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (MalformedObjectNameException e) {
					e.printStackTrace();
				} catch (ReflectionException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (InstanceNotFoundException e) {
					e.printStackTrace();
				} catch (MBeanException e) {
					e.printStackTrace();
				}
				nextJmx = nextJmx + 5000;
			}
			if (now > nextDisks) {
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
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextDisks = nextDisks + 60000;
			}
		}
		session.close(StatusCode.NORMAL, "I'm done");
	}
	
	public JmxMessage getJmxStats() throws MalformedObjectNameException, IOException, ReflectionException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstanceNotFoundException, MBeanException {
		JmxMessage ret = new JmxMessage();
		ObjectName query = new ObjectName("java.lang:type=GarbageCollector,*");
		Set<ObjectInstance> gcs = mBeanServerConnection.queryMBeans(query, null);
		LinkedHashSet<String> poolNames = new LinkedHashSet<String>();
		for (ObjectInstance next : gcs) {
			GcInfo gi = new GcInfo();
			 try {
				gi.setCollectionCount(((Number)mBeanServerConnection.getAttribute(next.getObjectName(), "CollectionCount")).longValue());
				gi.setCollectionTime(((Number)mBeanServerConnection.getAttribute(next.getObjectName(), "CollectionTime")).longValue());
				ret.gcInfo.add(gi);
				String[] poolNameArr = (String[])mBeanServerConnection.getAttribute(next.getObjectName(), "MemoryPoolNames");
				for (String nextPool : poolNameArr) {
					if (!poolNames.contains(nextPool)) {
						poolNames.add(nextPool);
					}
				}
			 } catch (AttributeNotFoundException | InstanceNotFoundException
					| MBeanException | ReflectionException e) {
				e.printStackTrace();
			}
		}
		for (String nextPool : poolNames) {
			query = new ObjectName("java.lang:type=MemoryPool,name=" + nextPool);
			Set<ObjectInstance> memPool = mBeanServerConnection.queryMBeans(query, null);
			ObjectInstance next = memPool.iterator().next();
			try {
				ret.memoryPoolUsage.put(nextPool, (Long) ((CompositeDataSupport)mBeanServerConnection.getAttribute(next.getObjectName(), "Usage")).get("used"));
				ret.memoryPoolPeakUsage.put(nextPool, (Long) ((CompositeDataSupport)mBeanServerConnection.getAttribute(next.getObjectName(), "PeakUsage")).get("used"));
			} catch (AttributeNotFoundException | InstanceNotFoundException
					| MBeanException | ReflectionException e) {
				e.printStackTrace();
			}
		}
		query = new ObjectName("java.lang:type=Memory");
		Set<ObjectInstance> mem = mBeanServerConnection.queryMBeans(query, null);
		ObjectInstance next = mem.iterator().next();
		try {
			ret.setHeapMemory(((Long) ((CompositeDataSupport)mBeanServerConnection.getAttribute(next.getObjectName(), "HeapMemoryUsage")).get("used")).longValue());
			ret.setNonHeapMemory(((Long) ((CompositeDataSupport)mBeanServerConnection.getAttribute(next.getObjectName(), "NonHeapMemoryUsage")).get("used")).longValue());
		} catch (AttributeNotFoundException | InstanceNotFoundException
				| MBeanException | ReflectionException e) {
			e.printStackTrace();
		}
		query = new ObjectName("java.lang:type=MemoryPool,name=Code Cache");
		Set<ObjectInstance> cc = mBeanServerConnection.queryMBeans(query, null);
		next = cc.iterator().next();
		try {
			ret.setHeapMemory(((Long) ((CompositeDataSupport)mBeanServerConnection.getAttribute(next.getObjectName(), "Usage")).get("used")).longValue());
		} catch (AttributeNotFoundException | InstanceNotFoundException
				| MBeanException | ReflectionException e) {
			e.printStackTrace();
		}
		query = new ObjectName("java.lang:type=Threading");
		Set<ObjectInstance> tr = mBeanServerConnection.queryMBeans(query, null);
		next = tr.iterator().next();
		ThreadMXBean threadBean = JMX.newMBeanProxy(mBeanServerConnection, query, ThreadMXBean.class);
		ret.setLiveThreads(threadBean.getThreadCount());
		long[] ids = threadBean.getAllThreadIds();
		for (long nextId : ids) {
			CompositeDataSupport tInfo = (CompositeDataSupport)mBeanServerConnection.invoke(query, "getThreadInfo", new Object[] { Long.valueOf(nextId) }, new String[] {Long.TYPE.getName()} );
			ThreadInfoMessage tim = new ThreadInfoMessage();
			tim.setBlockedCount(((Long)tInfo.get("blockedCount")).longValue());
			tim.setBlockedTime(((Long)tInfo.get("blockedTime")).longValue());
			tim.setInNative(((Boolean)tInfo.get("inNative")).booleanValue());
			tim.setLockName((String)tInfo.get("lockName"));
			tim.setLockOwnerId(((Long)tInfo.get("lockOwnerId")).longValue());
			tim.setLockOwnerName((String)tInfo.get("lockOwnerName"));
			tim.setSuspended(((Boolean)tInfo.get("suspended")).booleanValue());
			tim.setThreadId(((Long)tInfo.get("threadId")).longValue());
			tim.setThreadName((String)tInfo.get("lockOwnerName"));
			tim.setThreadState(Thread.State.valueOf((String)tInfo.get("threadState")));
			tim.setWaitedCount(((Long)tInfo.get("waitedCount")).longValue());
			tim.setWaitedTime(((Long)tInfo.get("waitedTime")).longValue());
			ret.threads.add(tim);
		}
		
		query = new ObjectName("java.lang:type=Runtime");
		RuntimeMXBean  runtimeBean = JMX.newMBeanProxy(mBeanServerConnection, query, RuntimeMXBean.class);
		ret.setStartTime(runtimeBean.getStartTime());
		ret.setUptime(runtimeBean.getUptime());
		query = new ObjectName("java.lang:type=ClassLoading");
		ClassLoadingMXBean  clBean = JMX.newMBeanProxy(mBeanServerConnection, query, ClassLoadingMXBean.class);
		ret.setLoadedClassCount(clBean.getLoadedClassCount());
		ret.setUnloadedClassCount(clBean.getUnloadedClassCount());
		return ret;
	}
}
