package com.nitorcreations.deployer.messages;

import org.msgpack.annotation.Message;

@Message
public class CPU extends AbstractMessage {
	long user;
	long sys;
	long nice;
	long idle;
	long wait;
	long irq;
	long softIrq;
	long stolen;
	long total;

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

	public long getNice() {
		return nice;
	}

	public void setNice(long nice) {
		this.nice = nice;
	}

	public long getIdle() {
		return idle;
	}

	public void setIdle(long idle) {
		this.idle = idle;
	}

	public long getWait() {
		return wait;
	}

	public void setWait(long wait) {
		this.wait = wait;
	}

	public long getIrq() {
		return irq;
	}

	public void setIrq(long irq) {
		this.irq = irq;
	}

	public long getSoftIrq() {
		return softIrq;
	}

	public void setSoftIrq(long softIrq) {
		this.softIrq = softIrq;
	}

	public long getStolen() {
		return stolen;
	}

	public void setStolen(long stolen) {
		this.stolen = stolen;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}	
}
