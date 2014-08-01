package com.nitorcreations.deployer;

import static com.nitorcreations.deployer.PropertyKeys.PROPERTY_KEY_LAUNCH_BINARY;
import static com.nitorcreations.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_LAUNCH_ARGS;

import java.util.Properties;

public class NativeLauncher extends AbstractLauncher implements LaunchMethod {

	@Override
	public void setProperties(Properties properties) {
		super.setProperties(properties);
		launchArgs.add(properties.getProperty(PROPERTY_KEY_LAUNCH_BINARY));
		addLauncherArgs(properties, PROPERTY_KEY_PREFIX_LAUNCH_ARGS);
	}


}
