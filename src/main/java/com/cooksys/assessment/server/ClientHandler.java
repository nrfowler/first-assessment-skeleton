package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.Date;
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
				String cmd=message.getCommand();
				Date d;
				System.out.println(raw);
				if(cmd.equals("connect")){
						log.info("user <{}> connected", message.getUsername());
						users.put(message.getUsername(),socket);
						Collection<Socket> keys = users.values();
						d= new Date();
						message.setContents(d.toString()+": <"+message.getUsername()+"> has connected");
						for(Socket s: keys){
							writer=new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
				}
				if(cmd.equals("disconnect")){
						log.info("user <{}> disconnected", message.getUsername());
						this.socket.close();
						users.remove(message.getUsername(),socket);
						Collection<Socket> keys1 = users.values();
						d= new Date();
						message.setContents(d.toString()+": <"+message.getUsername()+"> has disconnected");
						for(Socket s: keys1){
							writer=new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
				}
						if(cmd.equals("echo")){
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						d= new Date();
						message.setContents(d.toString()+": <"+message.getUsername()+"> (echo): "+message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						}
						if(cmd.equals("users")){
						log.info(users.keySet().toString());
						d=new Date();
						message.setContents(d.toString()+": currently connected users: \n<"+String.join(">\n<", users.keySet())+">");
						writer.write(mapper.writeValueAsString(message));
						writer.flush();
						}
						if(cmd.equals("broadcast")){
						Collection<Socket> keys11 = users.values();
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
						d=new Date();
						message.setContents(d.toString()+": <"+message.getUsername()+"> (all): "+message.getContents());
						for(Socket s: keys11){
							writer=new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
						}
						if(cmd.startsWith("@")){
						String addressee=cmd.substring(1);
						d= new Date();
						if(users.containsKey(addressee)&&users.get(addressee).isConnected()){
							log.info("sending message to : " +addressee);
							message.setContents(d.toString()+": <"+message.getUsername()+"> (whisper): "+message.getContents());
							writer=new PrintWriter(new OutputStreamWriter(users.get(addressee).getOutputStream()));
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
						}

				}
			

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
