package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	private Socket socket;
	private String username;
	static Map<String, SocketWriter> users = new HashMap<String, SocketWriter>();
	private PrintWriter writer;
	private ObjectMapper mapper = new ObjectMapper();

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}

	/**
	 * Sends a message to all users on the server.
	 * 
	 * @param contents:
	 *            String to concatenate to date and username
	 * @param message:
	 *            Message object that was received from client
	 * @param mapper:
	 *            ObjectMapper for converting message to JSON
	 * @throws IOException
	 */
	private void sendAll(String contents, Message message) throws IOException {
		PrintWriter w;
		Date d1 = new Date();
		message.setContents(d1.toString() + ": <" + message.getUsername() + contents);
		for (SocketWriter s : users.values()) {
			w = s.getWriter();
			w.write(mapper.writeValueAsString(message));
			w.flush();
		}
	}

	public void run() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				String cmd = message.getCommand();
				Date d;
				System.out.println(raw);
				if (cmd.equals("connect")) {
					username = message.getUsername();
					username = username.replaceAll("\\s", "_");
					log.info("username is now: " + username);
					if (users.containsKey(username)) {
						username = username + "1";
					}
					log.info("user <{}> connected", username);
					users.put(username, new SocketWriter(socket, new PrintWriter(new OutputStreamWriter(socket.getOutputStream()))));
					message.setUsername(username);
					sendAll("> has connected", message);
				}
				else if (cmd.equals("disconnect")) {
					log.info("user <{}> disconnected", message.getUsername());
					this.socket.close();
					if (users.remove(message.getUsername(), socket)) {
						sendAll("> has disconnected", message);
					}
				}
				else if (cmd.equals("echo")) {
					log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
					d = new Date();
					message.setContents(
							d.toString() + ": <" + message.getUsername() + "> (echo): " + message.getContents());
					String response = mapper.writeValueAsString(message);
					writer.write(response);
					writer.flush();
				}
				else if (cmd.equals("users")) {
					log.info(username + " got list of users: " + users.keySet().toString());
					d = new Date();
					message.setContents(d.toString() + ": currently connected users: \n<"
							+ String.join(">\n<", users.keySet()) + ">");
					writer.write(mapper.writeValueAsString(message));
					writer.flush();
				}
				else if (cmd.equals("broadcast")) {
					log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
					sendAll("> (all): " + message.getContents(), message);
				}
				else if (cmd.startsWith("@")) {
					String addressee;
					if (cmd.length() == 1) {
						String[] contents;
						contents = message.getContents().split(" ", 2);
						addressee = contents[0];
						message.setContents(contents[1]);
					} else
						addressee = cmd.substring(1);
					d = new Date();
					if (users.containsKey(addressee) && users.get(addressee).getSocket().isConnected()) {
						log.info("sending message to : " + addressee);
						message.setContents(
								d.toString() + ": <" + message.getUsername() + "> (whisper): " + message.getContents());
						PrintWriter dmWriter = new PrintWriter(
								new OutputStreamWriter(users.get(addressee).getSocket().getOutputStream()));
						dmWriter.write(mapper.writeValueAsString(message));
						dmWriter.flush();
						//if user is DM someone other than himself, send a copy of DM to user
						if (!addressee.equals(message.getUsername())) {
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
					} else {
						message.setContents(d.toString() + ": <" + message.getUsername() + "> : User <" + addressee
								+ "> not found");
						PrintWriter dmWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
						dmWriter.write(mapper.writeValueAsString(message));
						dmWriter.flush();
					}
				}
				else{
					log.info("Command not recognized");
				}
			}
		} catch (SocketException e) {
			log.error("Socket connection error :/", e);
			log.info("user <{}> disconnected", username);
			try {
				this.socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			users.remove(username);
			Message message = new Message();
			message.setUsername(username);
			message.setCommand("disconnect");
			message.setContents("");
				try {
					sendAll("> has disconnected", message);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		
	catch(IOException e)
	{
		log.error("Something went wrong :/", e);
	}

}}


