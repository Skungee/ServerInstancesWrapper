package me.limeglass.skungeewrapper;

import me.limeglass.skungeewrapper.objects.SkungeeWrapperCommands;
import me.limeglass.skungeewrapper.objects.WrappedServer;

public class CommandProcessor {
	
	private final SkungeeWrapper instance;
	
	public CommandProcessor(SkungeeWrapper instance) {
		this.instance = instance;
	}
	
	public void executeCommand(String input) {
		if (input == null || input.isEmpty()) {
			//instance.log(null, "input null");
			return;
		}
		String[] arguments = input.split("\\s");
		if (arguments.length < 1) {
			//instance.log(null, "command required");
			return;
		} else if (arguments.length < 2) {
			//instance.log(null, "server required");
			return;
		} else {
			SkungeeWrapperCommands command = SkungeeWrapperCommands.valueOf(arguments[0]);
			if (command == null) {
				instance.log(null, "The command " + arguments[0] + " could not be found.");
				return;
			}
			String server = arguments[1];
			switch (command) {
				case STOP_ALL:
					instance.log(null, "Stopping all servers.");
					for (WrappedServer wrappedServer : instance.getServers().values()) {
						instance.log(null, "Stopping server \"" + wrappedServer.getServerName() + "\".");
						wrappedServer.shutdown();
					}
					break;
				case START:
					if (arguments.length < 6) {
						instance.log(null, "Invalid arguments! Arguments required in order: server name, server path, max memory, starting memory, and jar file name.");
						return;
					}
					instance.log(null, "Starting server: " + server);
					instance.getServers().put(server, new WrappedServer(server, arguments[2], arguments[3], arguments[4], arguments[5]));
					break;
				case STOP:
					instance.log(null, "Stopping server: " + server);
					if (instance.getServers().containsKey(server)) {
						instance.getServers().get(server).shutdown();
						instance.getServers().remove(server);
					} else {
						instance.log(null, "The server \"" + server + "\" is not online!");
					}
					break;
				case RESTART:
					instance.log(null, "Restarting server: " + server);
					if (instance.getServers().containsKey(server)) {
						WrappedServer restartServer = instance.getServers().get(server);
						//List<String> commands = restartServer.getCommands();
						//String path = restartServer.getPath();
						instance.log(null, "Restarting " + server + " in 10 seconds...");
						restartServer.shutdown();
						new Thread(new Runnable() {
							public void run() {
								try {
									Thread.sleep(10000L);
								} catch (InterruptedException e) {}
								instance.getServers().put(server, restartServer);
								//instance.getServers().put(server, new WrappedServer(commands, server, path));
							}
						}).start();
					}
					break;
				case KILL:
					instance.log(null, "Killing server: " + server);
					if (instance.getServers().containsKey(server)) {
						instance.getServers().get(server).kill();
					}
					break;
				case COMMAND:
					if (arguments.length < 3) {
						instance.log(null, "You must specify a command to forward to " + server); return;
					}
					//TODO
					break;
				case SHUTDOWN:
					instance.log(null, "Shutting down the SkungeeWrapper.");
					System.exit(0);
					break;
				default: 
					instance.log(null, "Invalid command: " + command);
					break;
				}
		}
	}
}
