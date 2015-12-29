package tankbattle.client.stub;

import org.json.*;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

final class Client
{
	class Type
	{
		public static final String CREATE = "create";
		public static final String JOIN = "join";
	}

	private static ArrayList<Tank> tankList = new ArrayList<Tank>();

	private enum State {
		MATCH_BEGIN, GAME_BEGIN, GAME_PLAY, GAME_END, MATCH_END
	}


	public static void main(String[] args)
	{
		try {
			Client.run(args);
		} catch (JSONException e ) {
			System.out.println("JSON error. Terminating...");
			System.out.println(e.getMessage());
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


		JSONObject gameState; // Blocking wait for game state example

		long time = System.currentTimeMillis();
		State state = State.MATCH_BEGIN;
		while (true)
		{
			System.out.println(state);
			gameState = comm.getJSONGameState();

			if (gameState.has("comm_type") && gameState.getString("comm_type").equals("MatchEnd")) {
				break; // if in any state we receive match end, quit
			}

			if (state == State.MATCH_BEGIN && gameState.has("comm_type") && gameState.getString("comm_type").equals("GAMESTATE")) {
				state = State.GAME_BEGIN; // if in match begin and we get GAMESTATE, then go to game begin
			}

			if (state == State.GAME_BEGIN) {
				// this state loads all of our tanks into new Tank objects (once at the beginning of each game)
				JSONArray players = gameState.getJSONArray("players");
				for (int i = 0; i < players.length(); i++) {
					if (players.getJSONObject(i).getString("name").equals(gameInfo.getTeamName())) {
						JSONArray tanks = players.getJSONObject(i).getJSONArray("tanks");
						for (int j = 0; j < tanks.length(); j++) {

							String tankID = tanks.getJSONObject(j).getString("id");

							Tank tank = new Tank(tankID, gameInfo);

							tankList.add(tank);

						}
					}
				}
				System.out.println(gameState.toString());

				if (tankList.size() > 0) // if we successfully added the tanks
					state = State.GAME_PLAY;
			}

			if (state == State.GAME_PLAY) {

				// if it's the end of the game, go to game end state
				if (gameState.has("comm_type") && gameState.getString("comm_type").equals("GAME_END")) {
					state = State.GAME_END;
					continue;
				}

				// make sure all current tanks still exist (they might have been destroyed)
				Iterator<Tank> iter = tankList.iterator();
				while (iter.hasNext()) {
					Tank tank = iter.next();

					JSONArray players = gameState.getJSONArray("players");
					for (int i = 0; i < players.length(); i++) {
						if (players.getJSONObject(i).getString("name").equals(gameInfo.getTeamName())) {
							JSONArray tanks = players.getJSONObject(i).getJSONArray("tanks");
							boolean found = false;
							for (int j = 0; j < tanks.length(); j++) {
								String tankID = tanks.getJSONObject(j).getString("id");
								if (tankID.equals(tank.tankID)) {
									found = true;
									break;
								}
							}
							if (!found) iter.remove();
						}
					}
				}

				// execute main game logic if tank exists
				iter = tankList.iterator();
				while (iter.hasNext()) {
					Tank tank = iter.next();

					tank.update(gameState);
					List<String> commands = new ArrayList<String>();
					commands.addAll(tank.movement());
					commands.addAll(tank.attack());

					for (String cmd : commands) {
						String response = comm.send(cmd, "msg");
					}

				}

				// add any new tanks that were destroyed
				JSONArray players = gameState.getJSONArray("players");
				for (int i = 0; i < players.length(); i++) {
					if (players.getJSONObject(i).getString("name").equals(gameInfo.getTeamName())) {
						JSONArray tanks = players.getJSONObject(i).getJSONArray("tanks");
						for (int j = 0; j < tanks.length(); j++) {
							boolean found = false;
							String tankID = tanks.getJSONObject(j).getString("id");
							for (Tank tank : tankList) {
								if (tankID.equals(tank.tankID)) {
									found = true;
									break;
								}
							}
							if (!found) tankList.add(new Tank(tankID, gameInfo));
						}
					}
				}

			}

			if (state == State.GAME_END) {
				// clear tanks
				tankList.clear();
				state = State.GAME_BEGIN;
			}

		}
		
		System.out.println("Match finished. Exiting...");
	}

	public static void printHelp()
	{
			System.out.println("usage: Client <ip address> <team-name> <password> <match-token>");
	}
}
