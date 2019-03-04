package me.limeglass.skungeewrapper.runnables;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;

import me.limeglass.skungeewrapper.SkungeeWrapper;
import me.limeglass.skungeewrapper.objects.WrappedServer;

public class ServerWatcher implements Runnable {
	private final SkungeeWrapper instance;
	private final WrappedServer server;
	private final Process process;

	private final OutputStream out;
	private boolean shutdown = false;
	private int frozen = 0;
	
	public ServerWatcher(WrappedServer server, Process process, OutputStream out) {
		instance = SkungeeWrapper.getInstance();
		this.process = process;
		this.server = server;
		this.out = out;
	}
	
	private void processCheck() {
		boolean enable = true;
		if (out != null && enable) {
			try {
				out.write(13);
				out.flush();
			} catch (IOException e) {
				instance.log(Level.WARNING, "Server \"" + server.getServerName() + "\" may be stopped or frozen.");
				frozen += 1;
			}
		}
	}
	
	public void run() {
		for (;;) {
			processCheck();
			if (!process.isAlive()) {
				if (frozen > 1) {
					instance.log(Level.WARNING, "Server \"" + server.getServerName() + "\" stopped or frozen! Shutting down.");
					shutdown = true;
				}
				if (!instance.getServers().containsValue(server)) {
					shutdown = true;
				}
				frozen += 1;
			}
			else if (frozen > 0) {
				frozen -= 1;
			}
			try {
				process.exitValue();
				out.close();
				shutdown = true;
			}
			catch (IllegalThreadStateException|IOException localIllegalThreadStateException) {}
			if (shutdown) {
				break;
			}
			try {
				Thread.sleep(10000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		instance.log(null, server.getServerName() + " shut down.");
		if (instance.getServers().containsValue(server)) {
			instance.getServers().remove(server.getServerName());
			instance.log(null, "managedServers: " + instance.getServers().toString());
		}
		server.killThreads();
		try {
			server.getInputStream().close();
			server.getOutputStream().close();
			server.getErrorStream().close();
		}
		catch (IOException localIOException) {}
	}
}