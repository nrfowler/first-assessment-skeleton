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
	// TODO: put static arraylist of writers, static reader/writer of own socket
	private static List<PrintWriter> writers = new ArrayList<PrintWriter>();

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
	private void sendAll(String contents, Message message, ObjectMapper mapper) throws IOException {
		if (writers.size() == 0) {
			Collection<Socket> keys = new MapWrapper().getMap().values();
			for (Socket s : keys) {
				writers.add(new PrintWriter(new OutputStreamWriter(s.getOutputStream())));
			}
		}
		Date d1 = new Date();
		message.setContents(d1.toString() + ": <" + message.getUsername() + contents);
		for (PrintWriter w : writers) {
			w.write(mapper.writeValueAsString(message));
			w.flush();
		}

	}

	public void run() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			HashMap<String, Socket> users = new MapWrapper().getMap();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

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
					users.put(username, socket);
					writers.add(new PrintWriter(new OutputStreamWriter(socket.getOutputStream())));
					message.setUsername(username);
					sendAll("> has connected", message, mapper);
				}
				if (cmd.equals("disconnect")) {
					log.info("user <{}> disconnected", message.getUsername());
					this.socket.close();
					if (users.remove(message.getUsername(), socket)) {
						sendAll("> has disconnected", message, mapper);
					}
				}
				if (cmd.equals("echo")) {
					log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
					d = new Date();
					message.setContents(
							d.toString() + ": <" + message.getUsername() + "> (echo): " + message.getContents());
					String response = mapper.writeValueAsString(message);
					writer.write(response);
					writer.flush();
				}
				if (cmd.equals("users")) {
					log.info(username + " got list of users: " + users.keySet().toString());
					d = new Date();
					message.setContents(d.toString() + ": currently connected users: \n<"
							+ String.join(">\n<", users.keySet()) + ">");
					writer.write(mapper.writeValueAsString(message));
					writer.flush();
				}
				if (cmd.equals("broadcast")) {
					log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
					sendAll("> (all): " + message.getContents(), message, mapper);
				}
				if (cmd.startsWith("@")) {
					String addressee;
					String[] contents;
					if (cmd.length() == 1) {
						contents = message.getContents().split(" ", 2);
						addressee = contents[0];
						message.setContents(contents[1]);
					} else
						addressee = cmd.substring(1);
					d = new Date();

					if (users.containsKey(addressee) && users.get(addressee).isConnected()) {
						log.info("sending message to : " + addressee);
						message.setContents(
								d.toString() + ": <" + message.getUsername() + "> (whisper): " + message.getContents());
						PrintWriter dmWriter = new PrintWriter(
								new OutputStreamWriter(users.get(addressee).getOutputStream()));
						dmWriter.write(mapper.writeValueAsString(message));
						dmWriter.flush();
						if (!addressee.equals(message.getUsername())) {
							writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
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
			}
		} catch (SocketException e) {
			log.error("Socket connection error :/", e);
			log.info("user <{}> disconnected", username);
			try {
				this.socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (new MapWrapper().getMap().remove(username, socket)) {
				Message message = new Message();
				message.setUsername(username);
				message.setCommand("disconnect");
				message.setContents("");
				try {
					sendAll("> has disconnected", message, mapper);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}

	}

}
