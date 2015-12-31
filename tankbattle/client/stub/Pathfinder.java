package tankbattle.client.stub;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Arrays;

public class Pathfinder {

    private static Pathfinder instance = null;

    private GameMap map;
    private Node end;
    private Node start;

    private PriorityQueue<Node> open = new PriorityQueue<Node>(100, new PathComparator());
    private ArrayList<Node> closed = new ArrayList<Node>();

    private Pathfinder() {}

    public static Pathfinder getInstance() {
        if (instance == null) {
            instance = new Pathfinder();
        }
        return instance;
    }

    private class Node {
        Tank.Vector position = new Tank.Vector();
        double cost;
        Node parent;
        boolean impassable = false;

        @Override
        public boolean equals(Object obj) {
            boolean same = false;
            if (obj != null && obj instanceof Node) {
                same = samePosition(this, (Node)obj) || this == obj;
            }
            return same;
        }
    }

    public class GameMap {
        public Node[][] nodes;
        private int map_width;
        private int map_height;
        public GameMap(int width, int height) {
            map_width = width;
            map_height = height;
            nodes = new Node[height][width];
            for (int row = 0; row < map_height; row++) {
                for (int col = 0; col < map_width; col++) {
                    nodes[row][col] = new Node();
                }
            }
        }
        public Node getNode(int x, int y) {
            return nodes[map_height - 1 - y][x];
        }
    }

    public void updateMap(JSONObject map) throws JSONException {
        int map_width = map.getJSONArray("size").getInt(0);
        int map_height = map.getJSONArray("size").getInt(1);

        this.map = new GameMap(map_width, map_height);

        // set the position data for all the nodes
        for (int y = 0; y < map_height; y++) {
            for (int x = 0; x < map_width; x++) {
                Node n = this.map.getNode(x, y);
                n.position.x = x;
                n.position.y = y;
            }
        }

        // set impassable terrain in our map
//        int impassable_count = 0;

        JSONArray terrain_objects = map.getJSONArray("terrain");
        for (int i = 0; i < terrain_objects.length(); i++) {
            JSONObject terrain = terrain_objects.getJSONObject(i);
            if (terrain.getString("type").equals("SOLID") || terrain.getString("type").equals("IMPASSABLE")) {
                JSONObject bounds = terrain.getJSONObject("boundingBox");
                int corner_x = bounds.getJSONArray("corner").getInt(0);
                int corner_y = bounds.getJSONArray("corner").getInt(1);
                int width = bounds.getJSONArray("size").getInt(0);
                int height = bounds.getJSONArray("size").getInt(1);
                System.out.println("corner_x: " + corner_x + ", corner_y: " + corner_y + ", width: " + width + ", height: " + height);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        Node n = this.map.getNode(corner_x + x, corner_y + y);
                        n.impassable = true;
//                        ++impassable_count;
                    }
                }
            }
        }

//        System.out.println("count 1: " + impassable_count);
//
//        impassable_count = 0;
//
//        for (Node[] row : this.map.nodes) {
//            System.out.println();
//            for (Node n : row) {
//                if (n.impassable) {
//                    System.out.print(1);
//                    ++impassable_count;
//                }
//                else System.out.print(0);
//            }
//        }
//
//        System.out.println("count 2: " + impassable_count);
    }

    private double distance(Node n1, Node n2) {
        return Math.sqrt(Math.pow(n1.position.x - n2.position.x, 2) + Math.pow(n1.position.y - n2.position.y, 2));
    }

    public double heuristic(Node n1, Node n2) {
        return distance(n1, n2);
    }

    public class PathComparator implements Comparator<Node> {
        @Override
        public int compare(Node n1, Node n2) {
            double n1_cost = n1.cost + heuristic(n1, end);
            double n2_cost = n2.cost + heuristic(n2, end);
            if (n1_cost > n2_cost) return 1;
            else if (n1_cost < n2_cost) return -1;
            else return 0;
        }
    }

    private boolean samePosition(Node n1, Node n2) {
        if (n1 == null | n2 == null) return true;
        return ((Math.abs(n1.position.x - n2.position.x) < 0.1) && (Math.abs(n1.position.y - n2.position.y) < 0.1));
    }

    private ArrayList<Node> findNeighbours(Node n) {
        ArrayList<Node> neighbours = new ArrayList<Node>();
        int x = (int)Math.round(n.position.x);
        int y = (int)Math.round(n.position.y);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int x_prime = x + i;
                int y_prime = y + j;
                if (x_prime == x && y_prime == y) continue;
                if (x_prime < 0 || !(x_prime < map.map_width) || y_prime < 0 || !(y_prime < map.map_height)) continue;
                neighbours.add(map.getNode(x_prime, y_prime));
            }
        }
        return neighbours;
    }

    public ArrayList<Tank.Vector> findPath(Tank.Vector start, Tank.Vector end) {

        open.clear();
        closed.clear();

        for (int row = 0; row < map.map_height; row++) {
            for (int col = 0; col < map.map_width; col++) {
                map.nodes[row][col].parent = null;
                map.nodes[row][col].cost = 0;
            }
        }

        this.start = new Node();
        this.end = new Node();
        this.start.position.x = start.x;
        this.start.position.y = start.y;
        this.end.position.x = end.x;
        this.end.position.y = end.y;

        open.add(this.start);
        System.out.println("start position: x: " + start.x + ", y:" + start.y);
        System.out.println("end position: x: " + end.x + ", y:" + end.y);
        System.out.println("size of queue: " + open.size());

        int count = 0;

        while (!samePosition(open.peek(), this.end)) {
            System.out.println("loop iteration: " + count);
            Node current = open.poll();
            System.out.println("current x: " + current.position.x + ", y: " + current.position.y);
            closed.add(current);
            ArrayList<Node> neighbours = findNeighbours(current);
//            System.out.println("num of neighbours: " + neighbours.size());
            for (int i = 0; i < neighbours.size(); i++) {
                Node neighbour = neighbours.get(i);
                if (neighbour.impassable) {
//                    System.out.println("impassable");
                    continue;
                }
                double cost = current.cost + distance(current, neighbour);
                if (open.contains(neighbour) && (neighbour.cost - cost > 1) ) {
//                    System.out.println("1");
                    open.remove(neighbour);
                }
                if (closed.contains(neighbour) && (neighbour.cost - cost > 1) ) {
                    closed.remove(neighbour);
//                    System.out.println("2");
                }
                if (!open.contains(neighbour) && !closed.contains(neighbour)) {
//                    System.out.println("3");
                    neighbour.cost = cost;
                    neighbour.parent = current;
                    open.add(neighbour);
                }
            }
//            for (Node neighbour : neighbours) {
//                if (neighbour.impassable) {
////                    System.out.println("impassable");
//                    continue;
//                }
//                double cost = current.cost + distance(current, neighbour);
//                if (open.contains(neighbour) && cost < neighbour.cost) {
////                    System.out.println("1");
//                    open.remove(neighbour);
//                }
//                else if (closed.contains(neighbour) && cost < neighbour.cost) {
//                    closed.remove(neighbour);
////                    System.out.println("2");
//                }
//                else {
////                    System.out.println("3");
//                    neighbour.cost = cost;
//                    neighbour.parent = current;
//                    open.add(neighbour);
//                }
//            }
//            System.out.println("end of loop iteration: " + count);
            ++count;
        }

        ArrayList<Tank.Vector> path = new ArrayList<Tank.Vector>();

        count = 0;
        while (!samePosition(open.peek().parent, this.start)) {
            System.out.println("parent finding count: " + count);
            path.add(open.poll().parent.position);
            if (open.peek().parent == null) break;
            ++count;
        }

        return path;
    }

}