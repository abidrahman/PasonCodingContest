package tankbattle.client.stub;

import org.json.JSONObject;

/**
 * Created by kevin on 15-12-26.
 */
public class Tank {

    private String tankID;
    private JSONObject gameState;

    public Tank(String tankID) {
        this.tankID = tankID;
    }

    public void update(JSONObject gameState) {
        this.gameState = gameState;
    }

    public void movement() {

    }

    public void attack() {

    }

}
