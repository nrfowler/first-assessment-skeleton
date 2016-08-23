package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	static HashMap<String,Socket> users=new HashMap<String,Socket>();
	private Socket socket;

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}
	
	private void connectionAlert(String status, Date d, Message message,ObjectMapper mapper) throws IOException{
		
		Collection<Socket> keys = users.values();
		d= new Date();
		PrintWriter clWriter;
		message.setContents(d.toString()+": <"+message.getUsername()+"> has connected");
		for(Socket s: keys){
			clWriter=new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
			clWriter.write(mapper.writeValueAsString(message));
			clWriter.flush();
		}
		
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
						PrintWriter clWriter;
						message.setContents(d.toString()+": <"+message.getUsername()+"> has connected");
						for(Socket s: keys){
							clWriter=new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
							clWriter.write(mapper.writeValueAsString(message));
							clWriter.flush();
						}
				}
				if(cmd.equals("disconnect")){
						log.info("user <{}> disconnected", message.getUsername());
						this.socket.close();
						users.remove(message.getUsername(),socket);
						Collection<Socket> keys1 = users.values();
						d= new Date();
						message.setContents(d.toString()+": <"+message.getUsername()+"> has disconnected");
						PrintWriter clWriter;
						for(Socket s: keys1){
							clWriter=new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
							clWriter.write(mapper.writeValueAsString(message));
							clWriter.flush();
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
						PrintWriter bWriter;
						for(Socket s: keys11){
							bWriter=new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
							bWriter.write(mapper.writeValueAsString(message));
							bWriter.flush();
						}
						}
						if(cmd.startsWith("@")){
							String addressee;
							String[] contents;
							if(cmd.length()==1){
								contents=message.getContents().split(" ");
								addressee=contents[0];
							}
							else
								addressee=cmd.substring(1);
						d= new Date();
						if(users.containsKey(addressee)&&users.get(addressee).isConnected()){
							log.info("sending message to : " +addressee);
							message.setContents(d.toString()+": <"+message.getUsername()+"> (whisper): "+message.getContents());
							PrintWriter dmWriter=new PrintWriter(new OutputStreamWriter(users.get(addressee).getOutputStream()));
							dmWriter.write(mapper.writeValueAsString(message));
							dmWriter.flush();
						}
						}
				}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
