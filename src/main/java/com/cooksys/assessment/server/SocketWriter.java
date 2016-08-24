package com.cooksys.assessment.server;

import java.io.PrintWriter;
import java.net.Socket;

public class SocketWriter {
	private Socket socket;
	private PrintWriter writer;
	
	public SocketWriter(Socket socket, PrintWriter writer) {
		super();
		this.socket = socket;
		this.writer = writer;
	}
	public Socket getSocket() {
		return socket;
	}
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	public PrintWriter getWriter() {
		return writer;
	}
	public void setWriter(PrintWriter writer) {
		this.writer = writer;
	}
}
