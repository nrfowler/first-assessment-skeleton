package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	static ConcurrentHashMap<String,Socket> users=new ConcurrentHashMap<String,Socket>();
	private Socket socket;

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}
	
	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				switch (message.getCommand()) {
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						users.put(message.getUsername(),socket);
						Collection<Socket> keys = users.values();
						message.setContents("user "+message.getUsername()+" connected");
						for(Socket s: keys){
							writer=new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						this.socket.close();
						users.remove(message.getUsername(),socket);
						Collection<Socket> keys1 = users.values();
						message.setContents("user "+message.getUsername()+" disconnected");
						for(Socket s: keys1){
							writer=new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "users":
						log.info(users.keySet().toString());
						message.setContents(users.keySet().toString());
						writer.write(mapper.writeValueAsString(message));
						writer.flush();
						break;
					case "broadcast":
						Collection<Socket> keys11 = users.values();
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
						for(Socket s: keys11){
							writer=new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
						break;
					case "dm":
						String[] contentSplit=message.getContents().split(" ",2);
						String addressee=contentSplit[0];
						if(users.get(addressee).isConnected()){
							message.setContents(contentSplit[1]);
							writer=new PrintWriter(new OutputStreamWriter(users.get(addressee).getOutputStream()));
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
						break;

				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
