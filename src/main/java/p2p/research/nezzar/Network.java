package p2p.research.nezzar;

import java.util.Random;

public abstract class Network {
    /**
     * The node count in the network.
     */
    public static int N = 10;
    /** The node count in the network that a node should connect to */
    public static int K = 3;
    /**
     * Time to live.
     */
    public static int TTL = 15;
    public static float DISABLE_RATE = 0.0f;
    public static float BAD_NODE_RATE = 0.0f;

    public Node[] nodes;
    public boolean[][] reachable;
    public boolean[][] connected;
    /**
     * ping distance will be initialized to [1, 100]
     */
    public int[][] ping_distances;

    protected Network() {
        init();
        construct();
    }
    private void init() {
        nodes = new Node[N];
        for (int i = 0; i < N ; i++) {
            nodes[i] = new Node(i, BAD_NODE_RATE);
        }
        reachable = new boolean[N][N];
        connected = new boolean[N][N];
        ping_distances = new int[N][N];
        Random rnd = new Random();
        for (int i = 0; i < N; i++) {
            reachable[i][i] = false;
            for (int j = i + 1; j < N; j++) {
                reachable[i][j] = rnd.nextFloat() > DISABLE_RATE;
                reachable[j][i] = reachable[i][j];
                ping_distances[i][j] = rnd.nextInt(100);
                ping_distances[j][i] = ping_distances[i][j];
            }
        }
    }

    protected abstract void construct();
}
