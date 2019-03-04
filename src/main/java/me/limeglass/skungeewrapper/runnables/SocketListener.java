package me.limeglass.skungeewrapper.runnables;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;

import me.limeglass.skungeewrapper.SkungeeWrapper;

public class SocketListener implements Runnable {
	
	private final SkungeeWrapper instance;
	private final Socket socket;
	
	private Scanner scanner;
	private PrintWriter out;
	private String clientIP = "";
	private String command = "";
	private long lastHeartbeat = System.currentTimeMillis();
	
	public boolean running = true;
	
	public SocketListener(SkungeeWrapper instance, Socket socket) {
		this.instance = instance;
		this.socket = socket;
		clientIP = socket.getInetAddress().getHostAddress();
		instance.getConnections().add(this);
		instance.log(null, "Control Client Connected.");
	}
	
	public void run() {
		try {
			scanner = new Scanner(socket.getInputStream());
			out = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (running) {
			if ((this.command = scanner.nextLine()) != null) {
				if (command.startsWith("+heartbeat")) {
					lastHeartbeat = System.currentTimeMillis();
				} else {
					instance.log(null, "Received Input: " + command);
					instance.getProcessor().executeCommand(command);
				}
			} else {
				try {
					Thread.sleep(2000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if (!isAlive()) {
				instance.log(Level.WARNING, "Nothing left to listen for, ending");
				return;
			}
		}
	}
	
	public void close() {
		try {
			socket.shutdownInput();
			socket.shutdownOutput();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		lastHeartbeat = 1L;
		running = false;
	}
	
	private boolean isAlive() {
		if ((!checkHeartbeat()) || (socket.isConnected()) || (socket.isInputShutdown()) || (socket.isOutputShutdown()) || (!socket.isClosed())) {
			return true;
		}
		instance.getConnections().remove(this);
		instance.log(null, "Control client disconnected.");
		return false;
	}
	
	public boolean checkHeartbeat() {
		return System.currentTimeMillis() - lastHeartbeat > 32000L;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public Scanner getInputScanner() {
		return scanner;
	}
	
	public PrintWriter getOutputWriter() {
		return out;
	}
	
	public String getClientIP() {
		return clientIP;
	}
}