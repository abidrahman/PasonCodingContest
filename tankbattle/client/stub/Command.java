package tankbattle.client.stub;

import org.json.JSONObject;
import org.json.JSONException;

final class Command
{
	class Key {
		private static final String TEAM_NAME = "team_name";
		private static final String PASSWORD = "password";
		public static final String CLIENT_TOKEN = "client_token";
		public static final String MESSAGE = "message";
		public static final String MSG = "msg";
		public static final String RESP = "resp";
		private static final String COMM_TYPE = "comm_type";
		private static final String MATCH_TOKEN = "match_token";
		private static final String TANK_ID = "tank_id";
		private static final String DIRECTION = "direction";
		private static final String DISTANCE = "distance";
		private static final String RADS = "rads";
		private static final String CONTROL = "control";
		

		class CommType {
			private static final String MATCH_CONNECT = "MatchConnect";
			private static final String MOVE = "MOVE";
			private static final String ROTATE = "ROTATE";
			private static final String TURRET_ROTATE = "ROTATE_TURRET";
			private static final String FIRE = "FIRE";
			private static final String STOP = "STOP";
			
		}
	}

	// MatchConnect
	public String getMatchConnectCommand(String teamName, String password, String matchToken)
	{
		JSONObject json = new JSONObject();

		try {
			json.put(Key.COMM_TYPE, Key.CommType.MATCH_CONNECT);
			json.put(Key.TEAM_NAME, teamName);
			json.put(Key.PASSWORD, password);
			json.put(Key.MATCH_TOKEN, matchToken);
		} catch(JSONException e) {
			System.err.println("[Command connectCommand] couldn't create command");
			return null;
		}

		return json.toString();
	}
	
	// Movement
	public string move(String tankID, String direction, int distance, String clientToken)
	{
		JSONObject json = new JSONObject();
		
		try {
			json.put(Key.TANK_ID, tankID);
			json.put(Key.COMM_TYPE, Key.CommType.MOVE);
			json.put(Key.DIRECTION, direction);
			json.put(Key.DISTANCE, distance);
			json.put(Key.CLIENT_TOKEN, clientToken);
		} catch(JSONException e) {
			System.err.println("[Command connectCommand] couldn't create command");
			return null;
		}
		
		return json.toString();
	}
	
	// Tank Rotation
	public string rotate(String tankID, String direction, int rads, String clientToken)
	{
		JSONObject json = new JSONObject();
		
		try {
			json.put(Key.TANK_ID, tankID);
			json.put(Key.COMM_TYPE, Key.CommType.ROTATE);
			json.put(Key.DIRECTION, direction);
			json.put(Key.RADS, rads);
			json.put(Key.CLIENT_TOKEN, clientToken);
		} catch(JSONException e) {
			System.err.println("[Command connectCommand] couldn't create command");
			return null;
		}
		
		return json.toString();
	}
	
	// Turret Rotation
	public string rotateTurret(String tankID, String direction, int rads, String clientToken)
	{
		JSONObject json = new JSONObject();
		
		try {
			json.put(Key.TANK_ID, tankID);
			json.put(Key.COMM_TYPE, Key.CommType.TURRET_ROTATE);
			json.put(Key.DIRECTION, direction);
			json.put(Key.RADS, rads);
			json.put(Key.CLIENT_TOKEN, clientToken);
		} catch(JSONException e) {
			System.err.println("[Command connectCommand] couldn't create command");
			return null;
		}
		
		return json.toString();
	}
	
	// Fire
	public string fire(String tankID, String clientToken)
	{
		JSONObject json = new JSONObject();
		
		try {
			json.put(Key.TANK_ID, tankID);
			json.put(Key.COMM_TYPE, Key.CommType.FIRE);
			json.put(Key.CLIENT_TOKEN, clientToken);
		} catch(JSONException e) {
			System.err.println("[Command connectCommand] couldn't create command");
			return null;
		}
		
		return json.toString();
	}
	
	// Stop
	public string stop(String tankID, String control, String clientToken)
	{
		JSONObject json = new JSONObject();
		
		try {
			json.put(Key.TANK_ID, tankID);
			json.put(Key.COMM_TYPE, Key.CommType.STOP);
			json.put(Key.CONTROL, control)
			json.put(Key.CLIENT_TOKEN, clientToken);
		} catch(JSONException e) {
			System.err.println("[Command connectCommand] couldn't create command");
			return null;
		}
		
		return json.toString();
	}
	

}

