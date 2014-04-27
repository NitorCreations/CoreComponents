package com.nitorcreations.deployer.messages;

import org.msgpack.annotation.Message;

@Message
public class ProcessCPU extends AbstractMessage {
	double percent;
	long lastTime;
	long startTime;
	long user;
	long sys;
	long total;
	public double getPercent() {
		return percent;
	}
	public void setPercent(double percent) {
		this.percent = percent;
	}
	public long getLastTime() {
		return lastTime;
	}
	public void setLastTime(long lastTime) {
		this.lastTime = lastTime;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getUser() {
		return user;
	}
	public void setUser(long user) {
		this.user = user;
	}
	public long getSys() {
		return sys;
	}
	public void setSys(long sys) {
		this.sys = sys;
	}
	public long getTotal() {
		return total;
	}
	public void setTotal(long total) {
		this.total = total;
	}
	
}
