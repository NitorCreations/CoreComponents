package com.nitorcreations.messages;

import org.msgpack.annotation.Message;

@Message
public class Memory extends AbstractMessage {
	long total;
	long ram;
	long used;
	long free;
	long actualUsed;
	long actualFree;
	double usedPercent;
	double freePercent;
	public long getTotal() {
		return total;
	}
	public void setTotal(long total) {
		this.total = total;
	}
	public long getRam() {
		return ram;
	}
	public void setRam(long ram) {
		this.ram = ram;
	}
	public long getUsed() {
		return used;
	}
	public void setUsed(long used) {
		this.used = used;
	}
	public long getFree() {
		return free;
	}
	public void setFree(long free) {
		this.free = free;
	}
	public long getActualUsed() {
		return actualUsed;
	}
	public void setActualUsed(long actualUsed) {
		this.actualUsed = actualUsed;
	}
	public long getActualFree() {
		return actualFree;
	}
	public void setActualFree(long actualFree) {
		this.actualFree = actualFree;
	}
	public double getUsedPercent() {
		return usedPercent;
	}
	public void setUsedPercent(double usedPercent) {
		this.usedPercent = usedPercent;
	}
	public double getFreePercent() {
		return freePercent;
	}
	public void setFreePercent(double freePercent) {
		this.freePercent = freePercent;
	}
	
}
