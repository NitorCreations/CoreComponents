package com.nitorcreations.messages;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class HashMessage extends AbstractMessage {

	public Map<String, String> map;
	
	public static HashMessage create(Map<String, Object> rubyHash) {
		HashMessage ret = new HashMessage();
		HashMap<String, String> map = new HashMap<String, String>();
		for (Entry<String, Object> next : rubyHash.entrySet()) {
			map.put(next.getKey(), next.getValue().toString());
		}
		ret.setMap(map);
		return ret;
	}
	
	public Map<String, String> getMap() {
		return map;
	}
	
	public void setMap(Map<String, String> map) {
		this.map = map;
	}
	
}
