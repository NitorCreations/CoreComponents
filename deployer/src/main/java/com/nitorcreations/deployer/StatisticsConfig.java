package com.nitorcreations.deployer;

import java.util.concurrent.TimeUnit;

public class StatisticsConfig {

	private final long intervalProcs;
	private final long intervalCpus;
	private final long intervalProcCpus;
	private final long intervalMem;
	private final long intervalJmx;
	private final long intervalDisks;

	public StatisticsConfig() {
		this.intervalProcs = TimeUnit.SECONDS.toMillis(5);
		this.intervalCpus = TimeUnit.SECONDS.toMillis(5);
		this.intervalProcCpus = TimeUnit.SECONDS.toMillis(5);
		this.intervalMem = TimeUnit.SECONDS.toMillis(5);
		this.intervalJmx = TimeUnit.SECONDS.toMillis(5);
		this.intervalDisks = TimeUnit.MINUTES.toMillis(1);
	}
	public StatisticsConfig(long intervalProcs
			, long intervalCpus
			, long intervalProcCpus
			, long intervalMem
			, long intervalJmx
			, long intervalDisks) {
		this.intervalProcs = intervalProcs;
		this.intervalCpus = intervalCpus;
		this.intervalProcCpus = intervalProcCpus;
		this.intervalMem = intervalMem;
		this.intervalJmx = intervalJmx;
		this.intervalDisks = intervalDisks;
	}

	public long getIntervalProcs() {
		return intervalProcs;
	}

	public long getIntervalCpus() {
		return intervalCpus;
	}

	public long getIntervalProcCpus() {
		return intervalProcCpus;
	}

	public long getIntervalMem() {
		return intervalMem;
	}

	public long getIntervalJmx() {
		return intervalJmx;
	}

	public long getIntervalDisks() {
		return intervalDisks;
	}

}
