package com.nitorcreations.deployer.messages;

import org.msgpack.annotation.Message;

@Message
public class DiskUsage extends AbstractMessage {
	String name;
	long total;
	long free;
	long used;
	long avail;
	long files;
	long freeFiles;
	long diskReads;
	long diskWrites;
	long diskReadBytes;
	long diskWriteBytes;
	double diskQueue;
	double diskServiceTime;
	double usePercent;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getTotal() {
		return total;
	}
	public void setTotal(long total) {
		this.total = total;
	}
	public long getFree() {
		return free;
	}
	public void setFree(long free) {
		this.free = free;
	}
	public long getUsed() {
		return used;
	}
	public void setUsed(long used) {
		this.used = used;
	}
	public long getAvail() {
		return avail;
	}
	public void setAvail(long avail) {
		this.avail = avail;
	}
	public long getFiles() {
		return files;
	}
	public void setFiles(long files) {
		this.files = files;
	}
	public long getFreeFiles() {
		return freeFiles;
	}
	public void setFreeFiles(long freeFiles) {
		this.freeFiles = freeFiles;
	}
	public long getDiskReads() {
		return diskReads;
	}
	public void setDiskReads(long diskReads) {
		this.diskReads = diskReads;
	}
	public long getDiskWrites() {
		return diskWrites;
	}
	public void setDiskWrites(long diskWrites) {
		this.diskWrites = diskWrites;
	}
	public long getDiskReadBytes() {
		return diskReadBytes;
	}
	public void setDiskReadBytes(long diskReadBytes) {
		this.diskReadBytes = diskReadBytes;
	}
	public long getDiskWriteBytes() {
		return diskWriteBytes;
	}
	public void setDiskWriteBytes(long diskWriteBytes) {
		this.diskWriteBytes = diskWriteBytes;
	}
	public double getDiskQueue() {
		return diskQueue;
	}
	public void setDiskQueue(double diskQueue) {
		this.diskQueue = diskQueue;
	}
	public double getDiskServiceTime() {
		return diskServiceTime;
	}
	public void setDiskServiceTime(double diskServiceTime) {
		this.diskServiceTime = diskServiceTime;
	}
	public double getUsePercent() {
		return usePercent;
	}
	public void setUsePercent(double usePercent) {
		this.usePercent = usePercent;
	}
}
