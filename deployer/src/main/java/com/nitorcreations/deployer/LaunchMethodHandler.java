package com.nitorcreations.deployer;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class LaunchMethodHandler extends OptionHandler<LaunchMethod> {

	public LaunchMethodHandler(CmdLineParser parser, OptionDef option,
			Setter<? super LaunchMethod> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		if (option.isArgument()) {
			String valueStr = params.getParameter(0).toUpperCase();
			LaunchMethod value = LaunchMethod.valueOf(valueStr);
			if (value == null) {
				throw new CmdLineException(owner, Messages.ILLEGAL_OPERAND.format(valueStr));
			}
			setter.addValue(value);
			return 1;
		} else {
			setter.addValue(LaunchMethod.UBERJAR);
			return 0;
		}
	}

	@Override
	public String getDefaultMetaVariable() {
		return null;
	}
}