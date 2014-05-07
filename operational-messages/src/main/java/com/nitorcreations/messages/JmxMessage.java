package com.nitorcreations.messages;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.msgpack.annotation.Message;

@Message
public class JmxMessage extends AbstractMessage {
	long heapMemory;
	long nonHeapMemory;
	long codeCache;
	int liveThreads;
	int loadedClassCount;
	long unloadedClassCount;
	long startTime;
	long uptime;
	
	public long getUnloadedClassCount() {
		return unloadedClassCount;
	}
	public void setUnloadedClassCount(long l) {
		this.unloadedClassCount = l;
	}
	public long getUptime() {
		return uptime;
	}
	public void setUptime(long uptime) {
		this.uptime = uptime;
	}
	public long getHeapMemory() {
		return heapMemory;
	}
	public void setHeapMemory(long heapMemory) {
		this.heapMemory = heapMemory;
	}
	public long getNonHeapMemory() {
		return nonHeapMemory;
	}
	public void setNonHeapMemory(long nonHeapMemory) {
		this.nonHeapMemory = nonHeapMemory;
	}
	public long getCodeCache() {
		return codeCache;
	}
	public void setCodeCache(long codeCache) {
		this.codeCache = codeCache;
	}
	public int getLiveThreads() {
		return liveThreads;
	}
	public void setLiveThreads(int liveThreads) {
		this.liveThreads = liveThreads;
	}
	public int getLoadedClassCount() {
		return loadedClassCount;
	}
	public void setLoadedClassCount(int loadedClassCount) {
		this.loadedClassCount = loadedClassCount;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public List<GcInfo> getGcInfo() {
		return gcInfo;
	}
	public void setGcInfo(List<GcInfo> gcInfo) {
		this.gcInfo = gcInfo;
	}
	public Map<String, Long> getMemoryPoolUsage() {
		return memoryPoolUsage;
	}
	public void setMemoryPoolUsage(Map<String, Long> memoryPoolUsage) {
		this.memoryPoolUsage = memoryPoolUsage;
	}
	public List<ThreadInfoMessage> threads = new ArrayList<ThreadInfoMessage>();
	public List<GcInfo> gcInfo = new ArrayList<GcInfo>();
	public Map<String, Long> memoryPoolUsage = new LinkedHashMap<String, Long>();
	public Map<String, Long> memoryPoolPeakUsage = new LinkedHashMap<String, Long>();
}
