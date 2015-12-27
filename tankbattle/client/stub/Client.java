package tankbattle.client.stub;

import org.json.*;
import org.zeromq.ZMQ;
import java.util.ArrayList;
import java.util.List;

final class Client
{
	class Type
	{
		public static final String CREATE = "create";
		public static final String JOIN = "join";
	}

	private static List<Tank> tankList = new ArrayList<Tank>();

	private enum State {
		MATCH_BEGIN, GAME_BEGIN, GAME_PLAY, GAME_END, MATCH_END
	}


	public static void main(String[] args)
	{
		try {
			Client.run(args);
		} catch (JSONException e ) {
			System.out.println("JSON error. Terminating...");
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

				if (tankList.size() > 0) // if we successfully added the tanks
					state = State.GAME_PLAY;
			}

			if (state == State.GAME_PLAY) {

				// if it's the end of the game, go to game end state
				if (gameState.has("comm_type") && gameState.getString("comm_type").equals("GAME_END")) {
					state = State.GAME_END;
					continue;
				}

				// execute main game logic
				for (Tank tank : tankList) {
					tank.update(gameState);
					List<String> commands = new ArrayList<String>();
					commands.addAll(tank.movement());
					commands.addAll(tank.attack());

					for (String cmd : commands) {
						String response = comm.send(cmd, "msg");
					}

				}

			}

			if (state == State.GAME_END) {
				// clear tanks
				tankList.clear();
				state = State.GAME_BEGIN;
			}

		}

//			while (!gameState.getString("comm_type").equals("MatchEnd")) {
//
//				long new_time = System.currentTimeMillis();
//				if (new_time - time > 3000 && gameState.has("map")) {
//					time = new_time;
//					System.out.println(gameState.toString(4));
//					JSONArray map_size = gameState.getJSONObject("map").getJSONArray("size");
//					System.out.println(map_size.get(0));
//					System.out.println(map_size.get(1));
//				}
//
//				if (gameState.has("players")) {
//					JSONArray players = gameState.getJSONArray("players");
//					for (int i = 0; i < players.length(); i++) {
//						if (players.getJSONObject(i).getString("name").equals(gameInfo.getTeamName())) {
//							JSONArray tanks = players.getJSONObject(i).getJSONArray("tanks");
//							for (int j = 0; j < tanks.length(); j++) {
//
//								String tankID = tanks.getJSONObject(j).getString("id");
//
//								String moveCommand = command.move(tankID, "FWD", 10, gameInfo.getClientToken());
//								comm.send(moveCommand, "comm_type");
//								String rotate_command = command.rotateTurret(tankID, "CCW", 1, gameInfo.getClientToken());
//								String fire_command = command.fire(tankID, gameInfo.getClientToken());
//								comm.send(rotate_command, "comm_type");
//								comm.send(fire_command, "comm_type");
//								String rotate_tracks_command = command.rotate(tankID, "CCW", 1, gameInfo.getClientToken());
//								comm.send(rotate_tracks_command, "comm_type");
//
//								JSONArray projectiles = tanks.getJSONObject(j).getJSONArray("projectiles");
//								for (int k = 0; k < projectiles.length(); k++) {
//									Object range = projectiles.getJSONObject(k).get("range");
//									System.out.println(range);
//								}
//
//							}
//						}
//					}
//				}
//
//				gameState = comm.getJSONGameState(); // Blocking wait for game state example
//			}

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
