package tankbattle.client.stub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

public class Tank {

    public String tankID;
    private JSONObject gameState;
    private Command command = new Command();
    private GameInfo gameInfo;

    Vector position = new Vector();
    double direction;

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

    class TankData {
        Vector position = new Vector();
        double direction;
        // add more
    }

    private List<Projectile> projectiles = new ArrayList<Projectile>();
    public TankData this_tank = new TankData();

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
        JSONArray players = gameState.getJSONArray("players");
        for (int i = 0; i < players.length(); i++) {
            if (players.getJSONObject(i).getString("name").equals(gameInfo.getTeamName())) {
                JSONArray tanks = players.getJSONObject(i).getJSONArray("tanks");
                for (int j = 0; j < tanks.length(); j++) {
                    JSONObject tank = tanks.getJSONObject(j);
                    String tankID = tank.getString("id");
                    if (tankID.equals(this.tankID)) {
                        this_tank.position.x = tank.getJSONArray("position").getDouble(0);
                        this_tank.position.y = tank.getJSONArray("position").getDouble(1);
                        this_tank.direction = tank.getDouble("tracks");
                    }
                }
            }
        }
    }

    private double distance(Vector v1, Vector v2) {
        return Math.sqrt(Math.pow(v1.x - v2.x, 2) + Math.pow(v1.y - v2.y, 2));
    }

    private double dotProduct(Vector v1, Vector v2) {
        return (v1.x * v2.x) + (v1.y * v2.y);
    }


    private void updateProjectiles() throws JSONException {

        this.projectiles.clear(); // clear old projectiles because I am lazy to optimize updating

        JSONArray players = gameState.getJSONArray("players");
        for (int i = 0; i < players.length(); i++) {
            JSONArray tanks = players.getJSONObject(i).getJSONArray("tanks");
            for (int j = 0; j < tanks.length(); j++) {
                JSONArray tank_projectiles = tanks.getJSONObject(j).getJSONArray("projectiles");
                for (int k = 0; k < tank_projectiles.length(); k++) {

                    //create projectile object and update with new info
                    JSONObject projectile = tank_projectiles.getJSONObject(k);
                    Projectile p = new Projectile();
                    p.id = projectile.getString("id");
                    p.position.x = projectile.getJSONArray("position").getDouble(0);
                    p.position.y = projectile.getJSONArray("position").getDouble(1);
                    p.direction = projectile.getDouble("direction");
                    p.speed = projectile.getInt("speed");
                    p.range = projectile.getDouble("range");

                    // add the projectile
                    this.projectiles.add(p);
                }
            }
        }
    }

    public List<String> movement() {
        List<String> commands = new ArrayList<String>();

        // calculate the necessary movement
        // return the Strings needed to issue the commands


        // check if any projectiles are headed towards this tank

        for (Projectile p : projectiles) {

            if (distance(this_tank.position, p.position) > p.range) continue;

            double distance;

            double a = -1.0 * (Math.tan(p.direction));
            double b = 1.0;
            double c = Math.tan(p.direction) * p.position.x - p.position.y;

            distance = Math.abs(a*this_tank.position.x + b*this_tank.position.y + c) / Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));

            if (distance < 2.5) {
                // calculate closest perpendicular direction
                double perp_direction1 = p.direction + Math.PI / 2;
                double perp_direction2 = p.direction - Math.PI / 2;
                double difference = Math.min(this_tank.direction - perp_direction1, this_tank.direction - perp_direction2);

                String cw = "CW";
                String ccw = "CCW";

                String rot;
                if (difference > 0) rot = cw;
                else rot = ccw;

                String rotate_tracks_command = command.rotate(tankID, rot, difference, gameInfo.getClientToken());
                commands.add(rotate_tracks_command);

                String moveCommand = command.move(tankID, "FWD", 2.5, gameInfo.getClientToken());
                commands.add(moveCommand);
            }

        }

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
