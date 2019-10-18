package p2p.research.nezzar.kad2;

import p2p.research.nezzar.Node;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

public class KadNode2 {
    public Node node;

    /**
     * big-endian
     */
    private List<Set<KadNode2>> contacts;

    public NetworkKad2 network;

    public List<KadNode2> recentlyNotifiedNodes;

    public KadNode2(Node n, NetworkKad2 network) {
        this.node = n;
        this.network = network;
        contacts = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            contacts.add(new HashSet<>());
        }
        contacts = Collections.unmodifiableList(contacts);
        recentlyNotifiedNodes = new LinkedList<>();
    }

    /**
     * 按照 big-endian 获得 BitSet 对象
     * @return
     */
    public BitSet getBits() {
        return BitSet.valueOf(reverse(node.address));
    }

    public String getBinaryString() {
        BitSet bits = getBits();
        return getBinaryString(bits);
    }

    public static String getBinaryString(BitSet bits) {
        StringBuilder sb = new StringBuilder(256);
        for (int i = 255; i >= 0; i--) {
            sb.append(bits.get(i) ? "1" : "0");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "KadNode(" + node.index + "):" + getBinaryString();
    }

    public static byte[] reverse(byte[] data) {
        byte[] temp = new byte[data.length];
        for (int i = 0; i <data.length; i++) {
            temp[i] = data[data.length - 1 - i];
        }
        return temp;
    }

    public static BitSet getXorDistance(KadNode2 a, KadNode2 b) {
        BitSet distance = a.getBits();
        distance.xor(b.getBits());
        return distance;
    }

    public static BitSet getXorDistance(KadNode2 a, BitSet b) {
        BitSet distance = a.getBits();
        distance.xor(b);
        return distance;
    }

    BigInteger getAddressValue() {
        byte[] buf = new byte[33];
        System.arraycopy(node.address, 0, buf, 1, 32);
        return new BigInteger(buf);
    }

    static BigInteger getBitsValue(BitSet bits) {
        byte[] temp = reverse(bits.toByteArray());
        byte[] buf = new byte[33];
        System.arraycopy(temp, 0, buf, buf.length - temp.length, temp.length);
        return new BigInteger(buf);
    }

    public static BigInteger getAbsoluteDistance(KadNode2 a, KadNode2 b) {
        BigInteger aValue = a.getAddressValue();
        BigInteger bValue = b.getAddressValue();
        return aValue.subtract(bValue).abs();
    }

    public static BigInteger getAbsoluteDistance(KadNode2 a, BitSet b) {
        BigInteger aValue = a.getAddressValue();
        BigInteger bValue = getBitsValue(b);
        return aValue.subtract(bValue).abs();
    }

    public static BigInteger getXorDistanceValue(BitSet bits) {
        return getBitsValue(bits);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KadNode2 kadNode = (KadNode2) o;
        return node.index == kadNode.node.index;
    }

    @Override
    public int hashCode() {
        return node.index;
    }

    public BigInteger getXorDistanceValueFrom(KadNode2 another) {
        BitSet distance = getXorDistance(this, another);
        return getXorDistanceValue(distance);
    }

    public BigInteger getAbsoluteDistanceValueFrom(KadNode2 another) {
        return getAbsoluteDistance(this, another);
    }

    public BigInteger getAbsoluteDistanceValueFrom(BitSet another) {
        return getAbsoluteDistance(this, another);
    }
    public static int getBitIndex(KadNode2 a, KadNode2 b) {
        BitSet distance = KadNode2.getXorDistance(a, b);
        return distance.previousSetBit(255);
    }

    public static int getBitIndex(KadNode2 a, BitSet b) {
        BitSet distance = KadNode2.getXorDistance(a, b);
        return distance.previousSetBit(255);
    }

    public boolean isAlreadyKnown(KadNode2 node) {
        if (recentlyNotifiedNodes.contains(node)) {
            return true;
        }
        recentlyNotifiedNodes.add(node);
        // TODO 5 is a magic number
        if (recentlyNotifiedNodes.size() > 5) {
            recentlyNotifiedNodes.remove(0);
        }
        return false;
    }

    /**
     * 从本地节点寻找目标节点在bitIndex位上的联系人
     * @param target 目标节点
     * @param bitIndex 目标节点的bitIndex位
     * @return 寻找一个目标节点在bitIndex位上的联系人，如果没有则返回null
     */
    public KadNode2 getNextContact(KadNode2 target, int bitIndex) {
        BitSet bits = target.getBits();
        bits.flip(bitIndex);
        KadNode2 queryResult = this;
        while (queryResult != null) {
            if (getBitIndex(target, queryResult) == bitIndex) {
                break;
            }
            queryResult = queryResult.getNextJump(bits, target);
        }
        return queryResult;
    }

    /**
     * 返回导向目标节点的最优路径。
     * @param target 目标节点
     * @return 首先查询联系人表，返回对应bit位列表中ping值最低的节点。
     * 如果对应bit位为空，则寻找一个比自己更接近目标节点的节点。
     * 如果自己是最接近目标节点的节点，则返回 null。
     */
    public KadNode2 getNextJump(BitSet target) {
        return getNextJump(target, null);
    }

    /**
     * 返回导向目标节点的最优路径。
     * @param target 目标节点
     * @param avoid 避开的目标
     * @return 首先查询联系人表，返回对应bit位列表中ping值最低的节点。
     * 如果对应bit位为空，则寻找一个比自己更接近目标节点的节点。
     * 如果自己是最接近目标节点的节点，则返回 null。
     */
    public KadNode2 getNextJump(BitSet target, KadNode2 avoid) {
        int bitIndex = KadNode2.getBitIndex(this, target);
        Set<KadNode2> indexedContacts = new HashSet<>();
        indexedContacts.addAll(contacts.get(bitIndex));
        if (avoid != null) {
            indexedContacts.remove(avoid);
        }
        if (!indexedContacts.isEmpty()) {
            for (KadNode2 indexedContact : indexedContacts) {
                if (indexedContact.getBits().equals(target)) {
                    return indexedContact;
                }
            }
            return indexedContacts.stream().reduce((a,b)->{
                int aPing = network.getPingDistance(this.node.index, a.node.index);
                int bPing = network.getPingDistance(this.node.index, b.node.index);
                return aPing < bPing ? a : b;
            }).get();
        } else {
            Set<KadNode2> others = new HashSet<>();
            for (int i = 0; i < bitIndex; i++) {
                others.addAll(contacts.get(i));
            }
            others.add(this);
            if (avoid != null) {
                others.remove(avoid);
            }
            KadNode2 theOther = others.stream().reduce((a, b)->{
                BigInteger disA = a.getAbsoluteDistanceValueFrom(target);
                BigInteger disB = b.getAbsoluteDistanceValueFrom(target);
                return disA.compareTo(disB) < 0 ? a : b;
            }).get();
            if (theOther == this) {
                return null;
            } else {
                return theOther;
            }
        }
    }

    public boolean addAt(int bitIndex, KadNode2 another) {
        return contacts.get(bitIndex).add(another);
    }

    public int sizeAt(int bitIndex) {
        return contacts.get(bitIndex).size();
    }

    public Collection<KadNode2> dataAt(int bitIndex) {
        return Collections.unmodifiableSet(contacts.get(bitIndex));
    }

    public boolean removeAt(int bitIndex, KadNode2 node) {
        return contacts.get(bitIndex).remove(node);
    }

    public Stream<KadNode2> getAllContactsWithStream() {
        return contacts.stream().flatMap(c->c.stream());
    }

    public boolean isEmptyAt(int bitIndex) {
        return contacts.get(bitIndex).isEmpty();
    }
}
