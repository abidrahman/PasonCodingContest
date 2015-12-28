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

    private static final String CW = "CW";
    private static final String CCW = "CCW";

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
        double turret;
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
        this.update_enemy();
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
                        this_tank.turret = tank.getDouble("turret");
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
                if (tanks.getJSONObject(j).getString("id").equals(tankID)) continue; // do not dodge our own projectiles
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
                System.out.println(tankID + " " + this_tank.direction);
                System.out.println(p.direction);
                System.out.println(difference);

                String rotation;
                if (difference > 0) rotation = CW;
                else {
                    rotation = CCW;
                    difference = -difference;
                }

                String rotate_tracks_command = command.rotate(tankID, rotation, difference, gameInfo.getClientToken());
                commands.add(rotate_tracks_command);

                String moveCommand = command.move(tankID, "FWD", 2.5, gameInfo.getClientToken());
                commands.add(moveCommand);
            }

        }

        return commands;
    }
    
    ArrayList<Vector> enemy_tank_coordinates = new ArrayList<>();

    private void update_enemy() throws JSONException {

        this.enemy_tank_coordinates.clear();

        if (gameState.has("players")) {
            JSONArray players = gameState.getJSONArray("players");
            for (int i = 0; i < players.length(); i++) {

                if (!players.getJSONObject(i).getString("name").equals(gameInfo.getTeamName())) {
                    JSONArray enemy_tanks = players.getJSONObject(i).getJSONArray("tanks");
                    for (int j = 0; j < enemy_tanks.length(); j++) {
                        JSONArray tank_coordinate = enemy_tanks.getJSONObject(j).getJSONArray("position");
                        Vector coords = new Vector();
                        coords.x = tank_coordinate.getInt(0);
                        coords.y = tank_coordinate.getInt(1);
                        this.enemy_tank_coordinates.add(coords);
                    }
                }
            }
        }
    }

    Vector enemy = new Vector();
    double closest_distance;

    private Double find_closest_enemy() throws JSONException {

        closest_distance = 1280;

        if (gameState.has("players")) {
            JSONArray players = gameState.getJSONArray("players");
            for (int i = 0; i < players.length(); i++) {
                if (players.getJSONObject(i).getString("name").equals(gameInfo.getTeamName())) {
                    JSONArray my_tanks = players.getJSONObject(i).getJSONArray("tanks");
                    for (int j = 0; j < my_tanks.length(); j++) {
                        JSONArray my_tank_coordinate = my_tanks.getJSONObject(j).getJSONArray("position");
                        Vector my_coords = new Vector();
                        my_coords.x = my_tank_coordinate.getInt(0);
                        my_coords.y = my_tank_coordinate.getInt(1);

                        for (Vector e : enemy_tank_coordinates) {
                            double distance = distance(e, my_coords);
                            if (distance < closest_distance) {
                                closest_distance = distance;
                                enemy.x = e.x;
                                enemy.y = e.y;
                            }
                        }

                        //Calculate closest enemy's position relative to ours.
                        double Ox = enemy.x - this_tank.position.x;
                        double Oy = enemy.y - this_tank.position.y;
                        System.out.println(Ox);
                        System.out.println(Oy);

                        double angle_needed = Math.PI*(Math.atan(Oy/Ox)/180);
                        double angle_difference = 0;
                        double current_angle = this_tank.turret;

                        //Calculate angle difference depending on quadrant enemy is in.
                        //angle_difference is the angle (in rad) between the current turret angle and needed turret angle
                        //counting CLOCKWWISE FROM turret angle TO needed turret angle.

                        //Top-Right QUAD
                        if (angle_needed > 0 && enemy.y >= 0) {
                            if (current_angle > angle_needed) angle_difference = current_angle - angle_needed;
                            else angle_difference = 2*Math.PI - (angle_needed - current_angle);
                        }

                        //Top and Bottom Left QUADS
                        if ((angle_needed < 0 && enemy.y > 0) || (angle_needed > 0 && enemy.y <= 0)) {
                            if (current_angle > (Math.PI + angle_needed)) angle_difference = current_angle - (Math.PI + angle_needed);
                            else angle_difference = 2*Math.PI - ((Math.PI + angle_needed) - current_angle);
                        }

                        //Bottom-Right QUAD
                        if (angle_needed < 0 && enemy.y < 0) {
                            if (current_angle > (2*Math.PI + angle_needed)) angle_difference = current_angle - (2*Math.PI + angle_needed);
                            else angle_difference = 2*Math.PI - ((2*Math.PI + angle_needed) - current_angle);
                        }

                        return angle_difference;


                    }
                }
            }
        }

        return 0.0;
    }

    public List<String> attack() throws JSONException {
        List<String> commands = new ArrayList<String>();

        // calculate the necessary attack (turret rotate, fire)
        // return the Strings needed to issue the commands

        update_enemy();
        double closest_enemy = find_closest_enemy();
        
        if (closest_enemy <= 0.2 || closest_enemy >= 2*Math.PI) {
            String fire_command = command.fire(tankID, gameInfo.getClientToken());
            commands.add(fire_command);
        } else if (closest_enemy < Math.PI && closest_enemy > 0.2) {
            String rotate_command = command.rotateTurret(tankID, "CW", closest_enemy, gameInfo.getClientToken());
            commands.add(rotate_command);
        } else if (closest_enemy >= Math.PI && closest_enemy < 2*Math.PI) {
            String rotate_command = command.rotateTurret(tankID, "CCW", 2*Math.PI - closest_enemy, gameInfo.getClientToken());
            commands.add(rotate_command);
        }
        

        

        return commands;
    }

}
