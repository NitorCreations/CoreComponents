package com.nitorcreations.deployer;

import java.util.Properties;
import static com.nitorcreations.deployer.PropertyKeys.*;

public class NativeLauncher extends AbstractLauncher implements LaunchMethod {

	@Override
	public void setProperties(Properties properties) {
		super.setProperties(properties);
		launchArgs.add(properties.getProperty(PROPERTY_KEY_LAUNCH_BINARY));
		addLauncherArgs(properties, PROPERTY_KEY_PREFIX_LAUNCH_ARGS);
	}


}
