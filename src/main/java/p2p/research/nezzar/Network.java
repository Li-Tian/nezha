package p2p.research.nezzar;

import java.util.*;

public abstract class Network {
    /**
     * The node count in the network.
     */
    //public static int N = 10;
    protected final int N;
    /** The node count in the network that a node should connect to */
    //public static int K = 3;
    protected final int K;
    /**
     * Time to live.
     */
    public static int TTL = 15;
    public static float DISABLE_RATE = 0.0f;
    public static float BAD_NODE_RATE = 0.0f;

    public Node[] nodes;

    private static class XYVector {
        int from;
        int to;

        public XYVector(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XYVector xyVector = (XYVector) o;
            return from == xyVector.from &&
                    to == xyVector.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

    private HashSet<XYVector> unreachable;
    public boolean isReachable(int from, int to) {
        return !unreachable.contains(new XYVector(from, to));
    }
    public void setReachable(int from, int to, boolean reach) {
        if (!reach) {
            unreachable.add(new XYVector(from, to));
        } else {
            unreachable.remove(new XYVector(from, to));
        }
    }

    private HashSet<XYVector> connected;
    public boolean isConnected(int from, int to) {
        return connected.contains(new XYVector(from, to));
    }
    public void setConnected(int from, int to, boolean c) {
        if (c) {
            connected.add(new XYVector(from, to));
        } else {
            connected.remove(new XYVector(from, to));
        }
    }

    private HashSet<XYVector> pingConnected;
    public boolean isPingConnected(int from, int to) {
        return pingConnected.contains(new XYVector(from, to));
    }
    public void setPingConnected(int from, int to, boolean c) {
        if (c) {
            pingConnected.add(new XYVector(from, to));
        } else {
            pingConnected.remove(new XYVector(from, to));
        }
    }

    private Random rnd;
    /**
     * ping getDistance will be initialized to [1, 100]
     */
    private HashMap<XYVector, Integer> pingDistances;
    public int getPingDistance(int from, int to) {
        XYVector vector = new XYVector(from, to);
        if (!pingDistances.containsKey(vector)) {
            pingDistances.put(vector, rnd.nextInt(100));
        }
        return pingDistances.get(vector);
    }

    protected Network(int n, int k) {
        N = n;
        K = k;
        initNodes();
        initMatrix();
    }

    protected Network(int n, int k, Iterator<String> values) {
        N = n;
        K = k;
        initNodes3(values);
        initMatrix();
    }

    private void initNodes() {
        nodes = new Node[N];
        for (int i = 0; i < N; i++) {
            nodes[i] = new Node(i, BAD_NODE_RATE);
        }
    }

    private void initNodes2(Iterator<Integer> values) {
        nodes = new Node[N];
        for (int i = 0; i < N; i++) {
            nodes[i] = new Node(i, values.next(), values.next());
        }
    }
    private void initNodes3(Iterator<String> values) {
        nodes = new Node[N];
        for (int i = 0; i < N; i++) {
            nodes[i] = new Node(i, values.next());
        }
    }
    private void initMatrix() {
        unreachable = new HashSet<>();
        connected = new HashSet<>();
        pingConnected = new HashSet<>();
        pingDistances = new HashMap<>();
        rnd = new Random();
        for (int i = 0; i < N; i++) {
            setReachable(i, i, false);
            for (int j = i + 1; j < N; j++) {
                setReachable(i, j, rnd.nextFloat() >= DISABLE_RATE);
                setReachable(j, i, isReachable(i, j));
            }
        }
    }

    public int getN() {
        return N;
    }
    public int getK() {
        return K;
    }

}
