package com.nitorcreations.deployer;

import static com.nitorcreations.deployer.PropertyKeys.*;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

import com.nitorcreations.messages.WebSocketTransmitter;


public abstract class AbstractLauncher implements LaunchMethod {
	protected final String PROCESS_IDENTIFIER = new BigInteger(130, new SecureRandom()).toString(32);
	protected final Set<String> launchArgs = new LinkedHashSet<String>();
	protected Properties launchProperties;
	
	protected WebSocketTransmitter transmitter;
	protected Process child;
	protected int returnValue=-1;
	protected Map<String, String> extraEnv = new HashMap<>();
	protected File workingDir;
	
	public long getProcessId() {
		Sigar sigar = new Sigar();
		long stopTrying = System.currentTimeMillis() + 1000 * 60;
		while (System.currentTimeMillis() < stopTrying) {
			try {
				ProcessQuery q = ProcessQueryFactory.getInstance().getQuery("Env." + ENV_KEY_DEPLOYER_IDENTIFIER + ".eq=" + PROCESS_IDENTIFIER);
				return q.findProcess(sigar);
			} catch (Throwable e) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}
			}
		}
		throw new RuntimeException("Failed to resolve pid");
	}

	@Override
	public void run() {
		launch(extraEnv, getLaunchArgs());
	}

	@Override
	public void setProperties(Properties properties) {
		launchProperties = properties;
		try {
			transmitter = WebSocketTransmitter.getSingleton(properties);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to initialize launcher", e);
		}
		String extraEnvKeys = properties.getProperty(PROPERTY_KEY_EXTRA_ENV_KEYS);
		if (extraEnvKeys != null) {
			for (String nextKey : extraEnvKeys.split(",")) {
				extraEnv.put(nextKey.trim(), properties.getProperty((nextKey.trim())));
			}
		}
		workingDir = new File(properties.getProperty(PROPERTY_KEY_WDIR, "."));
	}
	
	protected void launch(String ... args) {
		this.launch(new HashMap<String, String>(), args);
	}
	
	protected void launch(Map<String, String> extraEnv, String ... args) {
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.environment().putAll(System.getenv());
		pb.environment().putAll(extraEnv);
		pb.environment().put(ENV_KEY_DEPLOYER_IDENTIFIER, PROCESS_IDENTIFIER);
		pb.directory(workingDir);
		System.out.printf("Starting %s%n", pb.command().toString());
		try {
			child = pb.start();
			StreamLinePumper stdout = new StreamLinePumper(child.getInputStream(), transmitter, "STDOUT");
			StreamLinePumper stderr = new StreamLinePumper(child.getErrorStream(), transmitter, "STDERR");
			new Thread(stdout, "child-stdout-pumper").start();
			new Thread(stderr, "child-sdrerr-pumper").start();
			setReturnValue(child.waitFor());
		} catch (IOException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void stop() {
		if (child != null) {
			child.destroy();
		}
	}
	
	private synchronized void setReturnValue(int val) {
		returnValue = val;
	}
	public synchronized int getReturnValue() {
		return returnValue;
	}
	protected void addLauncherArgs(Properties properties, String prefix) {
		int i=1;
		String next = properties.getProperty(prefix + i);
		while (next != null) {
			launchArgs.add(next);
			next = properties.getProperty(prefix + ++i);
		}
	}
	protected String[] getLaunchArgs() {
		return launchArgs.toArray(new String[launchArgs.size()]);
	}
}
