package tankbattle.client.stub;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Tank {

    private String tankID;
    private JSONObject gameState;
    private Command command = new Command();
    private GameInfo gameInfo;

    public Tank(String tankID, GameInfo gameInfo) {
        this.tankID = tankID;
        this.gameInfo = gameInfo;
    }

    public void printID() {
        System.out.println(this.tankID);
    }

    public void update(JSONObject gameState) {
        this.gameState = gameState;
    }

    public List<String> movement() {
        List<String> commands = new ArrayList<String>();

        // calculate the necessary movement
        // return the Strings needed to issue the commands

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
