package p2p.research.nezzar.n2d;

import p2p.research.nezzar.Node;

import java.math.BigInteger;
import java.util.*;

public class Node2D {

    private static BigInteger _2pow256 = null;

    private static BigInteger get2pow256() {
        if (_2pow256 == null) {
            byte[] data = new byte[33];
            data[0] = 1;
            _2pow256 = new BigInteger(data);
        }
        return _2pow256;
    }

    public static BigInteger getDistance(Node n1, Node n2) {
        byte[] x1 = new byte[17];
        byte[] y1 = new byte[17];
        byte[] x2 = new byte[17];
        byte[] y2 = new byte[17];
        System.arraycopy(n1.address, 0, x1, 1, 16);
        System.arraycopy(n1.address, 16, y1, 1, 16);
        System.arraycopy(n2.address, 0, x2, 1, 16);
        System.arraycopy(n2.address, 16, y2, 1, 16);
        BigInteger bx1 = new BigInteger(x1);
        BigInteger by1 = new BigInteger(y1);
        BigInteger bx2 = new BigInteger(x2);
        BigInteger by2 = new BigInteger(y2);
        BigInteger dx1 = bx2.subtract(bx1).abs();
        BigInteger dx2 = get2pow256().subtract(dx1);
        BigInteger dx = dx1.min(dx2);
        BigInteger dy1 = by2.subtract(by1).abs();
        BigInteger dy2 = get2pow256().subtract(by1);
        BigInteger dy = dy2.min(dy1);
        return dx.multiply(dx).add(dy.multiply(dy));
    }

    public static Direction2D getDirection(Node2D reference, Node2D target) {
        Node n1 = reference.thisNode;
        Node n2 = target.thisNode;
        byte[] x1 = new byte[17];
        byte[] y1 = new byte[17];
        byte[] x2 = new byte[17];
        byte[] y2 = new byte[17];
        System.arraycopy(n1.address, 0, x1, 1, 16);
        System.arraycopy(n1.address, 16, y1, 1, 16);
        System.arraycopy(n2.address, 0, x2, 1, 16);
        System.arraycopy(n2.address, 16, y2, 1, 16);
        BigInteger bx1 = new BigInteger(x1);
        BigInteger by1 = new BigInteger(y1);
        BigInteger bx2 = new BigInteger(x2);
        BigInteger by2 = new BigInteger(y2);
        BigInteger dx = bx2.subtract(bx1);
        BigInteger dy = by2.subtract(by1);

        BigInteger half = get2pow256().shiftRight(1);
        BigInteger negHalf = half.negate();
        if (dx.compareTo(half) >= 0) {
            dx = dx.subtract(get2pow256());
        } else if (dx.compareTo(negHalf) <= 0) {
            dx = dx.add(get2pow256());
        }
        if (dy.compareTo(half) >= 0) {
            dy = dy.subtract(get2pow256());
        } else if (dy.compareTo(negHalf) <= 0) {
            dy = dy.add(get2pow256());
        }
        int signDx = dx.signum();
        int signDy = dy.signum();
        if(signDx > 0 && signDy >= 0) {
            return Direction2D.TopRight;
        } else if(signDx >= 0 && signDy < 0) {
            return Direction2D.BottomRight;
        } else if(signDx < 0 && signDy <= 0) {
            return Direction2D.BottomLeft;
        } else {
            return Direction2D.TopLeft;
        }
    }

    public final Node thisNode;
    public final Network2D network;

    private final Comparator<Node2D> pingComparator;
    private final Comparator<Node2D> distanceComparator;

    public TreeSet<Node2D> topLeftList;
    public TreeSet<Node2D> topRightList;
    public TreeSet<Node2D> bottomLeftList;
    public TreeSet<Node2D> bottomRightList;
    public TreeSet<Node2D> pingList;

    public Collection<Node2D> knownNodes;

    public Node2D(Node node, Network2D n2d) {
        thisNode = node;
        network = n2d;

        pingComparator = (o1, o2)->{
            Node n1 = o1.thisNode;
            Node n2 = o2.thisNode;
            if (n1.index == thisNode.index || n2.index == thisNode.index || n1.index == n2.index) {
                return 0;
            }
            boolean n1Reachable = network.isReachable(thisNode.index, n1.index);
            boolean n2Reachable = network.isReachable(thisNode.index, n2.index);
            int ping1 = network.getPingDistance(thisNode.index, n1.index);
            int ping2 = network.getPingDistance(thisNode.index, n2.index);
            if (!n1Reachable && !n2Reachable) {
                return 0;
            }
            if (!n1Reachable) {
                return 1;
            }
            if (!n2Reachable) {
                return -1;
            }
            return Integer.compare(ping1, ping2);
        };

        distanceComparator = (o1, o2)->{
            Node n1 = o1.thisNode;
            Node n2 = o2.thisNode;
            if (n1.index == thisNode.index || n2.index == thisNode.index || n1.index == n2.index) {
                return 0;
            }
            boolean n1Reachable = network.isReachable(thisNode.index, n1.index);
            boolean n2Reachable = network.isReachable(thisNode.index, n2.index);
            if (!n1Reachable && !n2Reachable) {
                return 0;
            }
            if (!n1Reachable) {
                return 1;
            }
            if (!n2Reachable) {
                return -1;
            }
            BigInteger dis1 = getDistance(thisNode, n1);
            BigInteger dis2 = getDistance(thisNode, n2);
            return dis1.compareTo(dis2);
        };

        pingList = new TreeSet<>(pingComparator);
        topLeftList = new TreeSet<>(distanceComparator);
        topRightList = new TreeSet<>(distanceComparator);
        bottomLeftList = new TreeSet<>(distanceComparator);
        bottomRightList = new TreeSet<>(distanceComparator);

        knownNodes = new HashSet<>();
    }

    /**
     * Query the nearest node to the target from different directions, within the knowledge of this node.
     * 在参照点(此节点)的连接点中查找距离目标节点四个象限中距离最小的节点，并将其返回。
     *
     * @param target the target
     * @return the result that are nearest to the target.
     * return null if this Node is the nearest node to the target within the knowledge of this node.
     */
    public Collection<Node2D> queryNearestNodes(Node2D target) {
        Collection<Node2D> candidates = new HashSet<>();
        candidates.addAll(topLeftList);
        candidates.addAll(topRightList);
        candidates.addAll(bottomLeftList);
        candidates.addAll(bottomRightList);
        Collection<Node2D> topLeftCandidates = new HashSet<>();
        Collection<Node2D> topRightCandidates = new HashSet<>();
        Collection<Node2D> bottomLeftCandidates = new HashSet<>();
        Collection<Node2D> bottomRightCandidates = new HashSet<>();
        candidates.forEach(node->{
            switch(getDirection(target, node)) {
                case TopLeft:
                    topLeftCandidates.add(node);
                    break;
                case TopRight:
                    topRightCandidates.add(node);
                    break;
                case BottomLeft:
                    bottomLeftCandidates.add(node);
                    break;
                default:
                    bottomRightCandidates.add(node);
            }
        });
        candidates.clear();
        Node2D candidate = getNearestCandidate(topLeftCandidates, target);
        if (candidate != null) {
            candidates.add(candidate);
        }
        candidate = getNearestCandidate(topRightCandidates, target);
        if (candidate != null) {
            candidates.add(candidate);
        }
        candidate = getNearestCandidate(bottomLeftCandidates, target);
        if (candidate != null) {
            candidates.add(candidate);
        }
        candidate = getNearestCandidate(bottomRightCandidates, target);
        if (candidate != null) {
            candidates.add(candidate);
        }
        return candidates;
    }

    private static Node2D getNearestCandidate(Collection<Node2D> candidates, Node2D target) {
        Node2D candidate = null;
        BigInteger distance = null;
        for (Node2D node2D : candidates) {
            BigInteger anotherDistance = getDistance(target.thisNode, node2D.thisNode);
            if (anotherDistance.compareTo(BigInteger.ZERO) == 0) {
                continue;
            }
            if (candidate == null || anotherDistance.compareTo(distance) < 0) {
                candidate = node2D;
                distance = anotherDistance;
            }
        }
        return candidate;
    }

    private void updateContact(Node2D node, TreeSet<Node2D> contacts, int limit) {
        contacts.add(node);
        network.setConnected(thisNode.index, node.thisNode.index, true);
        if (contacts.size() > limit) {
            Node2D removed = contacts.pollLast();
            network.setConnected(thisNode.index, removed.thisNode.index, false);
        }
    }

    private void updatePingContact(Node2D node, TreeSet<Node2D> contacts, int limit) {
        contacts.add(node);
        network.setPingConnected(thisNode.index, node.thisNode.index, true);
        if (contacts.size() > limit) {
            Node2D removed = contacts.pollLast();
            network.setPingConnected(thisNode.index, removed.thisNode.index, false);
        }
    }

    public void updateContact(Node2D node) {
        switch (getDirection(this, node)) {
            case TopLeft:
                updateContact(node, topLeftList, network.getK());
                break;
            case TopRight:
                updateContact(node, topRightList, network.getK());
                break;
            case BottomLeft:
                updateContact(node, bottomLeftList, network.getK());
                break;
            case BottomRight:
                updateContact(node, bottomRightList, network.getK());
                break;
        }
        updatePingContact(node, pingList, network.getK());
    }

    public void updateContacts(Collection<Node2D> nodes) {
        for (Node2D node : nodes) {
            updateContact(node);
        }
    }

    public Node2D getNearestNode() {
        Collection<Node2D> candidates = new HashSet<>();
        if (!topLeftList.isEmpty()) {
            candidates.add(topLeftList.first());
        }
        if (!topRightList.isEmpty()) {
            candidates.add(topRightList.first());
        }
        if (!bottomLeftList.isEmpty()) {
            candidates.add(bottomLeftList.first());
        }
        if (!bottomRightList.isEmpty()) {
            candidates.add(bottomRightList.first());
        }
        return getNearestCandidate(candidates, this);
    }

    public void updateNeighbour() {
        Collection<Node2D> neighbour = new ArrayList<>();
        neighbour.addAll(pingList);
        neighbour.addAll(topLeftList);
        neighbour.addAll(topRightList);
        neighbour.addAll(bottomLeftList);
        neighbour.addAll(bottomRightList);
        neighbour.forEach(n->n.updateContact(this));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("Node[").append(thisNode.index).append("](").append(getShortX());
        sb.append(", ").append(getShortY()).append(") TL : (");
        topLeftList.forEach(n->sb.append(n.thisNode.index).append(","));
        sb.append(") TR : (");
        topRightList.forEach(n->sb.append(n.thisNode.index).append(","));
        sb.append(") BL : (");
        bottomLeftList.forEach(n->sb.append(n.thisNode.index).append(","));
        sb.append(") BR : (");
        bottomRightList.forEach(n->sb.append(n.thisNode.index).append(","));
        sb.append(")");
        return sb.toString();
    }

    private static final int VIEW_LENGTH = 2;

    public int getShortX() {
        byte[] buf = new byte[VIEW_LENGTH + 1];

        System.arraycopy(thisNode.address, 0, buf, 1, VIEW_LENGTH);
        BigInteger x = new BigInteger(buf);

        return x.intValue();
    }

    public int getShortY() {
        byte[] buf = new byte[VIEW_LENGTH + 1];

        System.arraycopy(thisNode.address, 16, buf, 1, VIEW_LENGTH);
        BigInteger y = new BigInteger(buf);
        return y.intValue();
    }

    public static int getShortWidth() {
        /*
        int base = 1;
        for (int i = 0; i < VIEW_LENGTH; i++) {
            base *= 256;
        }
         */
        return 65536;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node2D node2D = (Node2D) o;
        return thisNode.equals(node2D.thisNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(thisNode);
    }

}
