package p2p.research.nezzar;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

public class Node {
    private static Random rnd = new Random();
    public int index;
    public byte[] address = new byte[32];
    public boolean isBad;

    private Collection<Node> connectedFrom = new HashSet<Node>();
    private Collection<Node> connectedTo = new HashSet<Node>();

    public Node(int i) {
        index = i;
        rnd.nextBytes(address);
        isBad = false;
    }

    public Node(int i, float badNodeRate) {
        index = i;
        rnd.nextBytes(address);
        isBad = rnd.nextFloat() < badNodeRate;
    }

}
