package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	private Socket socket;
	private String username;
	static Map<String, SocketWriter> users = new HashMap<String, SocketWriter>();
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
		Date d1 = new Date();
		message.setContents(d1.toString() + ": <" + this.username + contents);
		for (SocketWriter s : users.values()) {
			s.getWriter().write(mapper.writeValueAsString(message));
			s.getWriter().flush();
		}
	}

	/**
	 * connect, disconnect, echo, direct message, print all users, broadcast
	 * message. On Socket Exception error, disconnect user.
	 */
	public void run() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				log.info(username+" ");
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				String cmd = message.getCommand();
				Date d;
				System.out.println(raw);
				if (cmd.equals("connect")) {
					username = message.getUsername();
					username = username.replaceAll("\\s", "_");
					if (users.containsKey(username)) {
						username = username + "1";
					}
					log.info("user <{}> connected", username);
					users.put(username, new SocketWriter(socket,
							new PrintWriter(new OutputStreamWriter(socket.getOutputStream()))));
					message.setUsername(username);
					sendAll("> has connected", message);
				} else if (cmd.equals("disconnect")) {
					log.info("user <{}> disconnected", this.username);
					this.socket.close();
					users.remove(this.username);
					sendAll("> has disconnected", message);
					break;
				} else if (cmd.equals("echo")) {
					log.info("user <{}> echoed message <{}>", this.username, message.getContents());
					d = new Date();
					message.setContents(
							d.toString() + ": <" + this.username + "> (echo): " + message.getContents());
					writer.write(mapper.writeValueAsString(message));
					writer.flush();
				} else if (cmd.equals("users")) {
					log.info(username + " got list of users: " + users.keySet().toString());
					d = new Date();
					message.setContents(d.toString() + ": currently connected users: \n<"
							+ String.join(">\n<", users.keySet()) + ">");
					writer.write(mapper.writeValueAsString(message));
					writer.flush();
				} else if (cmd.equals("broadcast")) {
					log.info("user <{}> broadcasted message <{}>", this.username, message.getContents());
					sendAll("> (all): " + message.getContents(), message);
				} else if (cmd.startsWith("@")) {
					String addressee;
					if (cmd.length() == 1) {
						String[] contents;
						contents = message.getContents().split(" ", 2);
						addressee = contents[0].replaceAll("@", "");
						message.setContents(contents[1]);
					} else
						addressee = cmd.substring(1);
					d = new Date();
					if (users.containsKey(addressee) && users.get(addressee).getSocket().isConnected()) {
						log.info("sending message to : " + addressee);
						message.setContents(
								d.toString() + ": <" + this.username + "> (whisper): " + message.getContents());
						PrintWriter dmWriter = new PrintWriter(
								new OutputStreamWriter(users.get(addressee).getSocket().getOutputStream()));
						dmWriter.write(mapper.writeValueAsString(message));
						dmWriter.flush();
						// if user is sending DM to another user, send a copy of
						// DM to the sender
						if (!addressee.equals(this.username)) {
							writer.write(mapper.writeValueAsString(message));
							writer.flush();
						}
					} else {
						message.setContents(d.toString() + ": <" + this.username + "> : User <" + addressee
								+ "> not found");
						writer.write(mapper.writeValueAsString(message));
						writer.flush();
					}
				} else {
					log.info("Command not recognized");
				}
			}
		} 
		//if socket exception, disconnect user and remove them from users map and close socket
		catch (SocketException e) {
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

		catch (IOException e) {
			log.error("Something went wrong :/", e);
		}


	}
}
