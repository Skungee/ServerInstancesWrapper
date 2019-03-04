package me.limeglass.skungeewrapper;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.Nullable;

import me.limeglass.skungeewrapper.runnables.SocketListener;
import me.limeglass.skungeewrapper.objects.WrappedServer;
import me.limeglass.skungeewrapper.utils.SkungeeLogger;

public class SkungeeWrapper {
	
	private final Map<String, WrappedServer> servers = new HashMap<String, WrappedServer>();
	static final Set<SocketListener> connections = new HashSet<SocketListener>();
	public final Set<Process> processes = new HashSet<Process>();
	private static SkungeeWrapper instance;
	private static SkungeeLogger logger;
	private Boolean debug = false, isWindows;
	private CommandProcessor processor;
	
	int abandonTime = 2;
	
	public SkungeeWrapper() {}
	
	public static void main(String[] args) {
		instance = new SkungeeWrapper();
		logger = new SkungeeLogger(instance, Logger.getLogger("SkungeeWrapper"));
		try {
			instance.run(args);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static SkungeeWrapper getInstance() {
		return instance;
	}
	
	public Boolean isWindows() {
		return isWindows;
	}

	public Boolean debug() {
		return debug;
	}
	
	public Map<String, WrappedServer> getServers() {
		return servers;
	}
	
	public Set<SocketListener> getConnections() {
		return connections;
	}
	
	public CommandProcessor getProcessor() {
		return processor;
	}
	
	public SkungeeLogger getLogger() {
		return logger;
	}
	
	public void log(@Nullable Level level, String... strings) {
		logger.log(level, strings);
	}
	
	private void safeShutdown() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				SkungeeWrapper.getInstance().shutdownAll();
			}
		});
	}
	
	private void shutdownAll() {
		//ut.log(Level.SEVERE, "Wrapper shutting down, stopping running servers.");
		for (WrappedServer server : servers.values()) {
			server.shutdown();
		}
		//Hault thread until all the servers finish shutting down.
		while (servers.size() >= 1) {
			try {
				Thread.sleep(3000L);
			}
			catch (InterruptedException localInterruptedException1) {}
		}
	}
	
	//TODO make a command input system within this run.
	private void run(String[] arguments) throws IOException {
		for (String argument : arguments) {
			if (argument.equalsIgnoreCase("debug")) debug = true;
		}
		safeShutdown();
		processor = new CommandProcessor(instance);
		if (arguments.length < 1) {
			//ut.log(Level.SEVERE, "Listen port must be specified!");
			System.exit(1);
		}
		try {
			listenPort = Integer.valueOf(arguments[0]).intValue();
		} catch (NumberFormatException e) {
			//ut.log(Level.SEVERE, "Specified port (" + args[0] + ") must be a number!");
			System.exit(1);
		}
		if (listenPort == 0) {
			//ut.log(Level.SEVERE, "Port cannot be 0!");
			System.exit(1);
		}
		String os = System.getProperty("os.name");
		instance.log(null, "Operating System: " + os + " v" + System.getProperty("os.version"));
		isWindows = os.matches("(?i)(.*)(windows)(.*)");
		watchAbandon();
		//ServerSocket serverSocket = new ServerSocket(listenPort);
		//ut.log("Listening on port " + listenPort);
		//while (!serverSocket.isClosed()) {
			//main.ut.log(getClass() + " is looping.");
		//	new Thread(new SocketListener(this, serverSocket.accept())).start();
		//}
	}

	private int listenPort;
	private void watchAbandon() {
		new Thread(new Runnable() {
			public void run() {
				int fails = 0;
				while (fails <= abandonTime * 4) {
					if (SkungeeWrapper.connections.size() > 0) {
						Iterator<SocketListener> iter = SkungeeWrapper.connections.iterator();
						while (iter.hasNext()) {
							SocketListener listener = iter.next();
							Socket sock = listener.getSocket();
							if (!sock.isConnected() || sock.isClosed() || listener.checkHeartbeat()) {
								listener.close();
								iter.remove();
								instance.log(null, "Control client disconnected, removed.");
							}
						}
					}
					if (instance.getServers().size() < 1 && instance.getConnections().size() < 1) {
						instance.log(Level.WARNING, "No connections and no servers running, shutting down in " + (abandonTime - fails / 4) + " more minute(s) without a connection.");
						fails += 1;
					} else {
						fails = 0;
					}
					try {
						Thread.sleep(15000L);
					}
					catch (InterruptedException localInterruptedException) {}
				}
				
				instance.log(null, "Wrapper had no connection or servers for " + abandonTime + " minutes, shutting down.");
				System.exit(0);
			}
		}).start();
	}
}