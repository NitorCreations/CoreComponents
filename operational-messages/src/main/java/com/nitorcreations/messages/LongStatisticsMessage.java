package com.nitorcreations.messages;

import java.util.Map;

public class LongStatisticsMessage extends AbstractMessage {
	public Map<String, Long> map;

	public Map<String, Long> getMap() {
		return map;
	}

	public void setMap(Map<String, Long> map) {
		this.map = map;
	}
}
