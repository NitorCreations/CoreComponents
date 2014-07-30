package com.nitorcreations.deployer;

import java.util.Properties;

public class NativeLauncher extends AbstractLauncher implements LaunchMethod {
	public static final String PROPERTY_KEY_LAUNCH_BINARY = "deployer.launch.binary";

	@Override
	public void setProperties(Properties properties) {
		super.setProperties(properties);
		launchArgs.add(properties.getProperty(PROPERTY_KEY_LAUNCH_BINARY));
		addLauncherArgs(properties, PROPERTY_KEY_PREFIX_LAUNCH_ARGS);
	}


}
