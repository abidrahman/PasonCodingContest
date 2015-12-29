package tankbattle.client.stub;


import java.util.Comparator;
import java.util.PriorityQueue;

public class Pathfinder {

    private static Pathfinder instance = null;

    private Pathfinder() {}

    public static Pathfinder getInstance() {
        if (instance == null) {
            instance = new Pathfinder();
        }
        return instance;
    }

    private class Vector {
        int x;
        int y;
    }

    private class Node {
        Vector position = new Vector();
        int cost;
        Node parent;
    }

    private Node end;
    private Node start;

    private int distance(Node n1, Node n2) {
        return (int)Math.round(Math.sqrt(Math.pow(n1.position.x - n2.position.x, 2) + Math.pow(n2.position.y - n2.position.y, 2)));
    }

    public int heuristic(Node n1, Node n2) {
        return distance(n1, n2);
    }

    private PriorityQueue<Node> open = new PriorityQueue<Node>(100, new PathComparator());

    public class PathComparator implements Comparator<Node> {
        @Override
        public int compare(Node n1, Node n2) {
            int n1_cost = n1.cost + heuristic(n1, end);
            int n2_cost = n2.cost + heuristic(n2, end);
            if (n1_cost > n2_cost) return 1;
            else if (n1_cost < n2_cost) return -1;
            else return 0;
        }
    }

}