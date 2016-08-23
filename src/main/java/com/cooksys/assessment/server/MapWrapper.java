package com.cooksys.assessment.server;

import java.net.Socket;
import java.util.HashMap;

public class MapWrapper {
	static HashMap<String, Socket> users = new HashMap<String, Socket>();
	HashMap<String, Socket> getMap(){
		return users;
	}
	
}
