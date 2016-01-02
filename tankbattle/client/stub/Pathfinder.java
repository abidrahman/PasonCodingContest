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
            if (x < 0 || !(x < map.map_width) || y < 0 || !(y < map.map_height)) return null;
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
        int impassable_count = 0;

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
                for (int y = -1; y <= height + 1; y++) {
                    for (int x = -1; x <= width + 1; x++) {
                        if (corner_x + x < 0 || !(corner_x + x < this.map.map_width) || corner_y + y < 0 || !(corner_y + y < this.map.map_height)) continue;
                        Node n = this.map.getNode(corner_x + x, corner_y + y);
                        n.impassable = true;
                        ++impassable_count;
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
        if (n1 == null || n2 == null) return false;
        return ((Math.abs(n1.position.x - n2.position.x) < 0.1) && (Math.abs(n1.position.y - n2.position.y) < 0.1));
    }

    private ArrayList<Node> findNeighbours(Node n, int radius) {
        ArrayList<Node> neighbours = new ArrayList<Node>();
        int x = (int)Math.round(n.position.x);
        int y = (int)Math.round(n.position.y);
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                int x_prime = x + i;
                int y_prime = y + j;
                if (x_prime == x && y_prime == y) continue;
//                if ((i == -1 && j == -1) || (i == 1 && j == 1) || (i == -1 && j == 1) || (i == 1 && j == -1)) continue;
                if (x_prime < 0 || !(x_prime < map.map_width) || y_prime < 0 || !(y_prime < map.map_height)) continue;
                if (closed.contains(map.getNode(x_prime, y_prime))) continue;
                if (map.getNode(x_prime, y_prime).impassable) continue;
                neighbours.add(map.getNode(x_prime, y_prime));
            }
        }
        return neighbours;
    }

    private int clamp(int num, int lower, int upper) {
        if (num < lower) num = lower;
        else if (num > upper) num = upper;
        return num;
    }

    private ArrayList<Node> findSuccessors(Node current) {
        ArrayList<Node> successors = new ArrayList<>();
        ArrayList<Node> neighbours = findNeighbours(current, 2);

        for (Node neighbour : neighbours) {
            int direction_x = clamp((int) Math.round(neighbour.position.x - current.position.x), -1, 1);
            int direction_y = clamp((int) Math.round(neighbour.position.y - current.position.y), -1, 1);

            Node jumpPoint = jump((int)Math.round(current.position.x), (int)Math.round(current.position.y), direction_x, direction_y, 0);
            if (jumpPoint != null) successors.add(jumpPoint);
        }
        if (successors.size() == 0) return neighbours;
        return successors;
    }

    private Node jump(int current_x, int current_y, int direction_x, int direction_y, int recursion_depth) {
        int nextX = current_x + direction_x;
        int nextY = current_y + direction_y;

        if (recursion_depth > 100) return null;

        if (nextX < 0 || !(nextX < map.map_width) || nextY < 0 || !(nextY < map.map_height)) return null;
        if (map.getNode(nextX, nextY).impassable) return null;
        if (samePosition(map.getNode(nextX, nextY), end)) return end;

        if (direction_y != 0 && direction_x != 0) {
            if (direction_x == 1 && direction_y == 1) {
                if (map.getNode(nextX - 1, nextY) != null) {
                    if (map.getNode(nextX - 1, nextY).impassable) {
                        return map.getNode(nextX, nextY);
                    }
                }
                if (map.getNode(nextX, nextY - 1) != null) {
                    if (map.getNode(nextX, nextY - 1).impassable) {
                        return map.getNode(nextX, nextY);
                    }
                }
            }
            if (direction_x == -1 && direction_y == 1) {
                if (map.getNode(nextX + 1, nextY) != null) {
                    if (map.getNode(nextX + 1, nextY).impassable) {
                        map.getNode(nextX, nextY);
                    }
                }
                if (map.getNode(nextX, nextY - 1) != null) {
                    if (map.getNode(nextX, nextY - 1).impassable) {
                        map.getNode(nextX, nextY);
                    }
                }
            }
            if (direction_x == 1 && direction_y == -1) {
                if (map.getNode(nextX - 1, nextY) != null) {
                    if (map.getNode(nextX - 1, nextY).impassable){
                        return map.getNode(nextX, nextY);
                    }
                }
                if (map.getNode(nextX, nextY + 1) != null) {
                    if (map.getNode(nextX, nextY + 1).impassable) {
                        return map.getNode(nextX, nextY);
                    }
                }
            }
            if (direction_x == -1 && direction_y == -1) {
                if (map.getNode(nextX + 1, nextY) != null) {
                    if (map.getNode(nextX + 1, nextY).impassable) {
                        return map.getNode(nextX, nextY);
                    }
                }
                if (map.getNode(nextX, nextY + 1) != null) {
                    if (map.getNode(nextX, nextY + 1).impassable) {
                        return map.getNode(nextX, nextY);
                    }
                }
            }
            if (jump(nextX, nextY, direction_x, 0, ++recursion_depth) != null ||
                    jump(nextX, nextY, 0, direction_y, ++recursion_depth) != null)
            {
                return map.getNode(nextX, nextY);
            }
        }
        else {
            if (direction_y == 0) { // horizontal case
                if (direction_x == 1) {
                    if (map.getNode(nextX, nextY + 1) != null && map.getNode(nextX + 1, nextY + 1) != null) {
                        if (map.getNode(nextX, nextY + 1).impassable && !map.getNode(nextX + 1, nextY + 1).impassable) {
                            return map.getNode(nextX, nextY);
                        }
                    }
                    if (map.getNode(nextX, nextY - 1) != null && map.getNode(nextX + 1, nextY - 1) != null) {
                        if (map.getNode(nextX, nextY - 1).impassable && !map.getNode(nextX + 1, nextY - 1).impassable) {
                            return map.getNode(nextX, nextY);
                        }
                    }
                }
                if (direction_x == -1) {
                    if (map.getNode(nextX, nextY + 1) != null && map.getNode(nextX - 1, nextY + 1) != null) {
                        if (map.getNode(nextX, nextY + 1).impassable && !map.getNode(nextX - 1, nextY + 1).impassable) {
                            return map.getNode(nextX, nextY);
                        }
                    }
                    if (map.getNode(nextX, nextY - 1) != null && map.getNode(nextX - 1, nextY - 1) != null) {
                        if (map.getNode(nextX, nextY - 1).impassable && !map.getNode(nextX - 1, nextY - 1).impassable) {
                            return map.getNode(nextX, nextY);
                        }
                    }
                }
            }
            else {
                if (direction_y == 1) {
                    if (map.getNode(nextX + 1, nextY) != null && map.getNode(nextX + 1, nextY + 1) != null) {
                        if (map.getNode(nextX + 1, nextY).impassable && !map.getNode(nextX + 1, nextY + 1).impassable) {
                            return map.getNode(nextX, nextY);
                        }
                    }
                    if (map.getNode(nextX - 1, nextY) != null && map.getNode(nextX - 1, nextY + 1) != null) {
                        if (map.getNode(nextX - 1, nextY).impassable && !map.getNode(nextX - 1, nextY + 1).impassable) {
                            return map.getNode(nextX, nextY);
                        }
                    }
                }
                if (direction_y == -1) {
                    if (map.getNode(nextX + 1, nextY) != null && map.getNode(nextX + 1, nextY - 1) != null) {
                        if (map.getNode(nextX + 1, nextY).impassable && !map.getNode(nextX + 1, nextY - 1).impassable) {
                            return map.getNode(nextX, nextY);
                        }
                    }
                    if (map.getNode(nextX - 1, nextY) != null && map.getNode(nextX - 1, nextY - 1) != null) {
                        if (map.getNode(nextX - 1, nextY).impassable && !map.getNode(nextX - 1, nextY - 1).impassable) {
                            return map.getNode(nextX, nextY);
                        }
                    }
                }
            }
        }
        return jump(nextX, nextY, direction_x, direction_y, ++recursion_depth);
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


        int count = 0;

        while (!samePosition(open.peek(), this.end)) {
//            System.out.println("loop iteration: " + count);
//            System.out.println("size of queue: " + open.size());
            if (count > 500) break;

            Node current = open.poll();
            if (current == null) break;
//            System.out.println("current x: " + current.position.x + ", y: " + current.position.y);
            closed.add(current);
            ArrayList<Node> neighbours = findSuccessors(current);
//            System.out.println(open.size());
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
            ++count;
        }

        System.out.println("total loops: " + count);

        ArrayList<Tank.Vector> path = new ArrayList<Tank.Vector>();

        if (open.peek() == null) {
            System.out.println("no valid path!");
            return path;
        }

        // reconstruct path to end
        count = 0;

        Node last = open.peek();
        while (last.parent != null) {
            path.add(last.position);
            last = last.parent;
        }
        return path;
    }

}