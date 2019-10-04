package p2p.research.nezzar.kad;

import p2p.research.nezzar.Node;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

public class KadNode {
    public Node node;

    /**
     * big-endian
     */
    public List<Set<KadNode>> contacts;

    public NetworkKad network;

    public List<KadNode> recentlyNotifiedNodes;

    public KadNode(Node n, NetworkKad network) {
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

    public static BitSet getXorDistance(KadNode a, KadNode b) {
        BitSet distance = a.getBits();
        distance.xor(b.getBits());
        return distance;
    }

    BigInteger getAddressValue() {
        byte[] buf = new byte[33];
        System.arraycopy(node.address, 0, buf, 1, 32);
        return new BigInteger(buf);
    }

    public static BigInteger getAbsoluteDistance(KadNode a, KadNode b) {
        BigInteger aValue = a.getAddressValue();
        BigInteger bValue = b.getAddressValue();
        return aValue.subtract(bValue).abs();
    }

    public static BigInteger getXorDistanceValue(BitSet bits) {
        byte[] temp = reverse(bits.toByteArray());
        byte[] buf = new byte[33];
        System.arraycopy(temp, 0, buf, buf.length - temp.length, temp.length);
        return new BigInteger(buf);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KadNode kadNode = (KadNode) o;
        return node.index == kadNode.node.index;
    }

    @Override
    public int hashCode() {
        return node.index;
    }

    public BigInteger getXorDistanceValueFrom(KadNode another) {
        BitSet distance = getXorDistance(this, another);
        return getXorDistanceValue(distance);
    }

    public BigInteger getAbsoluteDistanceValueFrom(KadNode another) {
        return getAbsoluteDistance(this, another);
    }

    public KadNode getFarthestContact(KadNode target) {
        Set<KadNode> allContacts = new HashSet<>();
        for (int i = 0; i < 256; i++) {
            allContacts.addAll(contacts.get(i));
        }
        allContacts.add(this);
        BinaryOperator<KadNode> farther = (a, b)->{
            BigInteger disA = a.getAbsoluteDistanceValueFrom(target);
            BigInteger disB = b.getAbsoluteDistanceValueFrom(target);
            return disA.compareTo(disB) < 0 ? b : a;
        };
        return allContacts.stream().reduce(farther).get();
    }

    // TODO 这里需要修改算法
    /**
     * 被询问方返回查询结果时，首先查看目标节点联系人列表中的所有的空bit位。
     * 查看本地所有节点，尽量满足填充目标的联系人列表中最高空bit位以下的更高的bit位（*）。
     * 如果没有办法填充目标的任何空bit位联系人列表，
     * 则返回当前节点的当前bit位联系人列表中，距离目标距离最大的节点（*）。
     * 如果当前节点的当前bit位联系人列表为空，则返回此节点已知联系人当中
     * 距离目标节点比自己近的最远的节点(*)。
     *
     * (*)选取节点时，优先选取距离目标节点(XOR距离,绝对地址距离)最大的节点。
     * 如果在 bit 位上的联系人列表中只存在目标节点，那么返回null
     */
    public KadNode getNextContact(KadNode target, int bitIndex) {
        Set<KadNode> sub = new HashSet<>();
        for (int i = 0; i <= 255; i++) {
            sub.addAll(contacts.get(i));
        }
        // 如果在 bit 位上的联系人列表中只存在目标节点，那么返回null
        sub.remove(target);

        // 选取节点时，优先选取距离目标节点(XOR距离,绝对地址距离)最大的节点。
        BinaryOperator<KadNode> farther = (a, b)->{
            int indexA = KadNode.getBitIndex(a, target);
            int indexB = KadNode.getBitIndex(b, target);
            if (indexA < indexB) {
                return b;
            } else if (indexA > indexB) {
                return a;
            }
            BigInteger disA = a.getAbsoluteDistanceValueFrom(target);
            BigInteger disB = b.getAbsoluteDistanceValueFrom(target);
            return disA.compareTo(disB) < 0 ? b : a;
        };

        KadNode queryResult = null;
//        // 被询问方返回查询结果时，首先查看目标节点联系人列表中的所有的空bit位。
//        // 查看本地所有节点，尽量满足填充目标的联系人列表中最高空bit位以下的更高的bit位（*）。
//        queryResult = sub.stream().filter(n->{
//            if (isAlreadyKnown(n)) {
//                return false;
//            }
//            return target.contacts.get(KadNode.getBitIndex(n, target)).isEmpty();
//        }).reduce(farther).orElse(null);
        // 如果没有办法填充目标的任何空bit位联系人列表，
        // 则返回当前节点的当前bit位联系人列表中，距离目标距离最大的节点（*）。
        if (queryResult == null) {
            queryResult = contacts.get(bitIndex).stream().reduce(farther).orElse(null);
        }
        // 如果当前节点的当前bit位联系人列表为空，则返回此节点已知联系人当中
        // 距离目标节点比自己近的最远的节点(*)。
        if (queryResult == null) {
            Predicate<KadNode> nearerThanThis = n->(farther.apply(this, n) == this);
            queryResult = sub.stream().filter(nearerThanThis).reduce(farther).orElse(null);
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
    public KadNode getNextJump(KadNode target) {
        int bitIndex = KadNode.getBitIndex(this, target);
        Set<KadNode> indexedContacts = contacts.get(bitIndex);
        if (!indexedContacts.isEmpty()) {
            if (indexedContacts.contains(target)) {
                return target;
            }
            return indexedContacts.stream().reduce((a,b)->{
                int aPing = network.getPingDistance(this.node.index, a.node.index);
                int bPing = network.getPingDistance(this.node.index, b.node.index);
                return aPing < bPing ? a : b;
            }).get();
        } else {
            Set<KadNode> others = new HashSet<>();
            for (int i = 0; i < bitIndex; i++) {
                others.addAll(contacts.get(i));
            }
            others.add(this);
            KadNode theOther = others.stream().reduce((a, b)->{
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

    public boolean isAlreadyKnown(KadNode node) {
        if (recentlyNotifiedNodes.contains(node)) {
            return true;
        }
        recentlyNotifiedNodes.add(node);
        // TODO 5 is magic
        if (recentlyNotifiedNodes.size() > 5) {
            recentlyNotifiedNodes.remove(0);
        }
        return false;
    }

    public static int getBitIndex(KadNode a, KadNode b) {
        BitSet distance = KadNode.getXorDistance(a, b);
        return distance.previousSetBit(255);
    }
}
