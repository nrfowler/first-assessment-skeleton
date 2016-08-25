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

	public synchronized Socket getSocket() {
		return socket;
	}

	public synchronized void setSocket(Socket socket) {
		this.socket = socket;
	}

	public synchronized PrintWriter getWriter() {
		return writer;
	}

	public synchronized void print(String msg) {
		this.writer.write(msg);
		this.writer.flush();

		return;
	}

	public synchronized void setWriter(PrintWriter writer) {
		this.writer = writer;
	}
}
