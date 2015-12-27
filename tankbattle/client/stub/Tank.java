package tankbattle.client.stub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Tank {

    private String tankID;
    private JSONObject gameState;
    private Command command = new Command();
    private GameInfo gameInfo;

    class Vector {
        double x;
        double y;
    }

    class Projectile {
        String id;
        Vector position = new Vector();
        double range;
        double direction;
        int speed;
    }

    private List<Projectile> projectiles = new ArrayList<Projectile>();

    public Tank(String tankID, GameInfo gameInfo) {
        this.tankID = tankID;
        this.gameInfo = gameInfo;
    }

    public void printID() {
        System.out.println(this.tankID);
    }

    public void update(JSONObject gameState) throws JSONException {
        this.gameState = gameState;
        this.updateProjectiles();

    }


    private void updateProjectiles() throws JSONException {
        projectiles.clear();
        JSONArray players = gameState.getJSONArray("players");
        for (int i = 0; i < players.length(); i++) {
            JSONArray tanks = players.getJSONObject(i).getJSONArray("tanks");
            for (int j = 0; j < tanks.length(); j++) {
                JSONArray tank_projectiles = tanks.getJSONObject(j).getJSONArray("projectiles");
                for (int k = 0; k < tank_projectiles.length(); k++) {
                    JSONObject projectile = tank_projectiles.getJSONObject(k);
                    Projectile p = new Projectile();
                    p.id = projectile.getString("id");
                    p.position.x = projectile.getJSONArray("position").getDouble(0);
                    p.position.y = projectile.getJSONArray("position").getDouble(1);
                    p.direction = projectile.getDouble("direction");
                    p.speed = projectile.getInt("speed");
                    p.range = projectile.getDouble("range");
                }
            }
        }
    }

    public List<String> movement() {
        List<String> commands = new ArrayList<String>();

        // calculate the necessary movement
        // return the Strings needed to issue the commands

        // check if any projectiles are headed towards this tank




        String moveCommand = command.move(tankID, "FWD", 10, gameInfo.getClientToken());
        commands.add(moveCommand);

        String rotate_tracks_command = command.rotate(tankID, "CCW", 1, gameInfo.getClientToken());
        commands.add(rotate_tracks_command);

        return commands;
    }

    public List<String> attack() {
        List<String> commands = new ArrayList<String>();

        // calculate the necessary attack (turret rotate, fire)
        // return the Strings needed to issue the commands

        String rotate_command = command.rotateTurret(tankID, "CCW", 1, gameInfo.getClientToken());
        commands.add(rotate_command);

        String fire_command = command.fire(tankID, gameInfo.getClientToken());
        commands.add(fire_command);

        return commands;
    }

}
