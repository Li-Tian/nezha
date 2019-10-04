package p2p.research.nezzar;

import java.util.BitSet;
import java.util.Random;

public class Node {
    private static Random rnd = new Random();
    public int index;
    /**
     * big-endian
     */
    public byte[] address = new byte[32];
    public boolean isBad;

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

    public Node(int i, int x, int y) {
        this(i);
        address[0] = (byte) (x >> 8);
        address[1] = (byte) x;
        address[16] = (byte) (y >> 8);
        address[17] = (byte) y;
    }

    public Node(int i, String bits) {
        index = i;
        BitSet bitData = new BitSet(256);
        for (int j = 0; j < 256; j++) {
            bitData.set(255 - j, bits.charAt(j)=='1');
        }
        byte[] temp = bitData.toByteArray();
        for (int j = 0; j < temp.length; j++) {
            address[31 - j] = temp[j];
        }
        isBad = false;
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return index == node.index;
    }
}
