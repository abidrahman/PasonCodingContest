package tankbattle.client.stub;

import org.json.*;
import org.zeromq.ZMQ;

final class Client
{
	class Type
	{
		public static final String CREATE = "create";
		public static final String JOIN = "join";
	}
	

	public static void main(String[] args)
	{
		try {
			Client.run(args);
		} catch (JSONException e) {
			System.out.println("JSON error.");
		}
	}

	public static void run(String[] args) throws JSONException
	{
		String ipAddress = null;
		String teamName = null;
		String password = null;
		String matchToken = null;

		if(args.length != 4) {
			Client.printHelp();
			return;
		}

		ipAddress = args[0];
		teamName = args[1];
		password = args[2];
		matchToken = args[3];

		System.out.println("Starting Battle Tank Client...");

		Command command = new Command();

		// retrieve the command to connect to the server
		String connectCommand = command.getMatchConnectCommand(teamName, password, matchToken);

		// retrieve the communication singleton
		Communication comm = Communication.getInstance(ipAddress, matchToken);

		// send the command to connect to the server
		System.out.println("Connecting to server...");
		String clientToken = comm.send(connectCommand, Command.Key.CLIENT_TOKEN);
		System.out.println("Received client token... " + clientToken);
		
		// Check to make sure we are connected
		if (null == clientToken)
		{
			System.out.println("Error: unable to connect!");
			System.exit(-1);
		}

		// the GameInfo object will hold the client's name, token, game type, etc.
		GameInfo gameInfo = new GameInfo(clientToken, teamName);

		// We are now connected to the server. Let's do some stuff:
		System.out.println("Connected!");
		
		System.out.println("Waiting for initial game state...");


		JSONObject gameState = comm.getJSONGameState(); // Blocking wait for game state example

		while (!gameState.getString("comm_type").equals("MatchEnd")) {
			if (gameState.has("players")) {
				JSONArray players = gameState.getJSONArray("players");
				for (int i = 0; i < players.length(); i++) {
					if (players.getJSONObject(i).getString("name").equals(gameInfo.getTeamName())) {
						JSONArray tanks = players.getJSONObject(i).getJSONArray("tanks");
						for (int j = 0; j < tanks.length(); j++) {
							String tankID = tanks.getJSONObject(j).getString("id");
							String rotate_command = command.rotateTurret(tankID, "CCW", 1, gameInfo.getClientToken());
							String fire_command = command.fire(tankID, gameInfo.getClientToken());
							comm.send(rotate_command, Command.Key.RESP);
							comm.send(fire_command, Command.Key.RESP);
						}
					}
				}
			}

			gameState = comm.getJSONGameState(); // Blocking wait for game state example
		}

		System.out.println("Received game state!");
		
		// Add your algorithm here
		System.out.println("Missing algorithm.");
		
		System.out.println("Exiting...");
	}

	public static void printHelp()
	{
			System.out.println("usage: Client <ip address> <team-name> <password> <match-token>");
	}
}
