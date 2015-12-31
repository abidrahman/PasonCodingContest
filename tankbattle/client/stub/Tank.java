package tankbattle.client.stub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

public class Tank {

    public String tankID;
    private JSONObject gameState;
    private Command command = new Command();
    private GameInfo gameInfo;
    private Pathfinder pathfinder = Pathfinder.getInstance();

    private static final String CW = "CW";
    private static final String CCW = "CCW";

    private Vector enemy = new Vector();
    private double closest_distance;
    ArrayList<Vector> my_tank_coordinates = new ArrayList<>();
    ArrayList<Vector> enemy_tank_coordinates = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<Projectile>();
    public TankData this_tank = new TankData();

    private enum State {
        DOGFIGHT, HUNTING
    }

    private State state = State.DOGFIGHT;

    public static class Vector {
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
        this.update_tanks();
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

    public ArrayList<String> strategy() throws JSONException {
        ArrayList<String> commands = new ArrayList<>();

        double distance = 0;
        double closest_d = 1000;
        Vector closest = new Vector();
        boolean found_enemy = false;
        for (Vector e : enemy_tank_coordinates) {
            distance = distance(e, this_tank.position);
            if ((distance < closest_d) && !doesCollide(this_tank.position, e)) {
                found_enemy = true;
                break;
            }
        }

        if (found_enemy) state = State.DOGFIGHT;
        if (!found_enemy)
            state = State.HUNTING;

        if (state == State.DOGFIGHT) {
            commands.addAll(dodgeProjectiles());
            commands.addAll(attack());
        }
        if (state == State.HUNTING) {
            commands.addAll(huntEnemy());
        }

        return commands;
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

    private ArrayList<String> dodgeProjectiles() {
        ArrayList<String> commands = new ArrayList<String>();

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

                String moveCommand = command.move(tankID, "FWD", 1, gameInfo.getClientToken());
                commands.add(moveCommand);
            }

        }

        return commands;
    }

    private ArrayList<String> huntEnemy() {
        // Move towards the closest enemy.
        ArrayList<String> commands = new ArrayList<String>();

        double distance = 0;
        double closest_d = 1000;
        Vector closest = new Vector();
        for (Vector e : enemy_tank_coordinates) {
            distance = distance(e, this_tank.position);
            if (distance < closest_d) {
                closest_d = distance;
                closest.x = e.x;
                closest.y = e.y;

            }
        }

        ArrayList<Vector> path = pathfinder.findPath(this_tank.position, closest);

        for (Vector coord : path) {
            System.out.println("Path: x:" + coord.x + ", y:" + coord.y);
        }

        if (path.size() > 3) {
            double x = path.get(path.size() - 2).x;
            if (x == 0) x = 0.000000001;
            double y = path.get(path.size() - 2).y;
            System.out.println("My position: x : " + this_tank.position.x + ", y: " + this_tank.position.y + " Path: x:" + x + ", y:" + y);

            double dir = this_tank.direction;
            double needed_dir = Math.atan2(y, x);

            double difference = dir - needed_dir;

            String rotation;
            if (difference > 0) rotation = CW;
            else {
                rotation = CCW;
                difference = -difference;
            }
            String rotate_tracks_command = command.rotate(tankID, rotation, difference, gameInfo.getClientToken());
            commands.add(rotate_tracks_command);

            String moveCommand = command.move(tankID, "FWD", 2, gameInfo.getClientToken());
            commands.add(moveCommand);
        }

        return commands;
    }

    private void update_tanks() throws JSONException {

        this.my_tank_coordinates.clear();

        if (gameState.has("players")) {
            JSONArray players = gameState.getJSONArray("players");
            for (int i = 0; i < players.length(); i++) {

                if (players.getJSONObject(i).getString("name").equals(gameInfo.getTeamName())) {
                    JSONArray enemy_tanks = players.getJSONObject(i).getJSONArray("tanks");
                    for (int j = 0; j < enemy_tanks.length(); j++) {
                        Vector coords = new Vector();
                        JSONArray tank_coordinate = enemy_tanks.getJSONObject(j).getJSONArray("position");
                        coords.x = tank_coordinate.getInt(0);
                        coords.y = tank_coordinate.getInt(1);
                        this.my_tank_coordinates.add(coords);
                    }
                }
            }
        }
    }

    private void update_enemy() throws JSONException {

        this.enemy_tank_coordinates.clear();

        if (gameState.has("players")) {
            JSONArray players = gameState.getJSONArray("players");
            for (int i = 0; i < players.length(); i++) {

                if (!players.getJSONObject(i).getString("name").equals(gameInfo.getTeamName())) {
                    JSONArray enemy_tanks = players.getJSONObject(i).getJSONArray("tanks");
                    for (int j = 0; j < enemy_tanks.length(); j++) {
                        Vector coords = new Vector();
                        JSONArray tank_coordinate = enemy_tanks.getJSONObject(j).getJSONArray("position");
                        coords.x = tank_coordinate.getInt(0);
                        coords.y = tank_coordinate.getInt(1);
                        this.enemy_tank_coordinates.add(coords);
                    }
                }
            }
        }
    }

    private boolean doesCollide(Vector start, Vector end) throws JSONException {
        //Returns true if projectile collides with an obstacle before the target

        if (gameState.has("map")) {
            JSONObject map = gameState.getJSONObject("map");
            JSONArray terrains = map.getJSONArray("terrain");
            for (int i = 0; i < terrains.length(); i++) {
                
                JSONObject obstacle = terrains.getJSONObject(i);
                if (obstacle.getString("type").equals("SOLID")) {

                    JSONObject bounds = obstacle.getJSONObject("boundingBox");
                    Vector obs_start = new Vector();
                    Vector obs_end = new Vector();
                    obs_start.x = (double)bounds.getJSONArray("corner").getInt(0);
                    obs_start.y = (double)bounds.getJSONArray("corner").getInt(1);
                    obs_end.x = obs_start.x + (double)bounds.getJSONArray("size").getInt(0);
                    obs_end.y = obs_start.y + (double)bounds.getJSONArray("size").getInt(1);

                    if (Line2D.linesIntersect(start.x,start.y,end.x,end.y,obs_start.x,obs_start.y,obs_end.x,obs_end.y) ||
                            Line2D.linesIntersect(start.x,start.y,end.x,end.y,obs_end.x,obs_start.y,obs_start.x,obs_end.y)) {
                        return true;
                    }
                }
            }
        }
        //If no collision return false
        return false;
    }

    private double find_closest_enemy() throws JSONException {

        double distance;
        closest_distance = 1280;
        enemy.x = 0;
        enemy.y = 0;

        for (Vector e : enemy_tank_coordinates) {
            distance = distance(e, this_tank.position);
            if ((distance < closest_distance) && !doesCollide(this_tank.position,e)) {
                closest_distance = distance;
                enemy.x = e.x;
                enemy.y = e.y;

            }
        }

        //Calculate closest enemy's position relative to ours.
        double Ox = enemy.x - this_tank.position.x;
        double Oy = enemy.y - this_tank.position.y;
        
        if (Ox == 0.0) Ox = 0.00001;
        double angle_needed = Math.atan(Oy/Ox);
        double current_angle = this_tank.turret;

        double angle_difference;

        //Calculate angle difference depending on quadrant enemy is in.
        //angle_difference is the angle (in rad) between the current turret angle and needed turret angle
        //counting CLOCKWISE FROM turret angle TO needed turret angle.

        //Top-Right QUAD
        //Nothing changes.

        //Top-Left QUAD & Bottom-left QUAD
        if ((angle_needed < 0 && Oy > 0) || (angle_needed >= 0 && Oy <= 0 && Ox < 0)) angle_needed = Math.PI + angle_needed;

        //Bottom-Right QUAD
        if (angle_needed < 0 && Oy < 0) angle_needed = 2*Math.PI + angle_needed;


        if (current_angle > angle_needed) angle_difference = current_angle - angle_needed;
        else angle_difference = 2*Math.PI - (angle_needed - current_angle);

        return angle_difference;
    }

    private boolean friendly_in_the_way(double closest_enemy_angle) {

        //Calculate if there is a friendly in the way before the enemy, if so don't shoot.
        for (Vector m : my_tank_coordinates) {

            double Mx = m.x - this_tank.position.x;
            double My = m.y - this_tank.position.y;
            double avoid_angle = Math.atan(My/Mx);

            //Top-Left QUAD & Bottom-left QUAD
            if ((avoid_angle < 0 && My > 0) || (avoid_angle > 0 && My <= 0)) avoid_angle = Math.PI + avoid_angle;
            //Bottom-Right QUAD
            if (avoid_angle < 0 && My < 0) avoid_angle = 2*Math.PI + avoid_angle;

            if ((avoid_angle < (closest_enemy_angle + 0.1) && avoid_angle > (closest_enemy_angle - 0.1)) && (closest_distance > distance(this_tank.position,m))) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> attack() throws JSONException {
        ArrayList<String> commands = new ArrayList<String>();

        // calculate the necessary attack (turret rotate, fire)
        // return the Strings needed to issue the commands

        //This is the difference between current angle and needed angle going CW
        double closest_enemy = find_closest_enemy();

        //Only shoot if aiming at target && within range && friendly NOT in the way
        if ((closest_enemy <= 0.05 || closest_enemy >= 2*Math.PI) && (closest_distance <= 100) && (!friendly_in_the_way(closest_enemy))) {
            String fire_command = command.fire(tankID, gameInfo.getClientToken());
            commands.add(fire_command);
        } else if (closest_enemy < Math.PI && closest_enemy > 0.05) {
            String rotate_command = command.rotateTurret(tankID, CW, closest_enemy, gameInfo.getClientToken());
            commands.add(rotate_command);
        } else if (closest_enemy >= Math.PI && closest_enemy < 2*Math.PI) {
            String rotate_command = command.rotateTurret(tankID, CCW, 2*Math.PI - closest_enemy, gameInfo.getClientToken());
            commands.add(rotate_command);
        }
                    

        return commands;
    }

}
