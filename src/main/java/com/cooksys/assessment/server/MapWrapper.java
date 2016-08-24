package com.cooksys.assessment.server;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MapWrapper {
	static Map<String, SocketWriter> users = new HashMap<String, SocketWriter>();
	Map<String, SocketWriter> getMap(){
		return users;
	}
	
}
