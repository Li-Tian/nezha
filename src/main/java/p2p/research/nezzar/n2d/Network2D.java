package p2p.research.nezzar.n2d;

import p2p.research.nezzar.Network;

import java.util.*;

public class Network2D extends Network {

    public List<Node2D> nodesInNetwork = new ArrayList<>();

    private int index = 0;

    public Network2D(int n, int k) {
        super(n, k);
    }

    public boolean construct() {
        // construct the network
        if (index >= N) {
            return false;
        }
        Node2D node = new Node2D(nodes[index], this);
        construct(node);
        index++;
        return true;
    }

    private void construct(Node2D target) {
        if (!nodesInNetwork.isEmpty()) {
            // 先从单一方向上寻找到最接近的节点。
            Node2D reference = nodesInNetwork.iterator().next();
            target.updateContact(reference);
            searchAndUpdate(reference, target);
            // 然后确认一下周围环境，从四个方向上确认最近的节点是最近的节点。
            Collection<Node2D> temp = new ArrayList<Node2D>();
            temp.addAll(target.topLeftList);
            for (Node2D ref : temp) {
                searchAndUpdate(ref, target);
            }
            temp.clear();
            temp.addAll(target.topRightList);
            for (Node2D ref : temp) {
                searchAndUpdate(ref, target);
            }
            temp.clear();
            temp.addAll(target.bottomLeftList);
            for (Node2D ref : temp) {
                searchAndUpdate(ref, target);
            }
            temp.clear();
            temp.addAll(target.bottomRightList);
            for (Node2D ref : temp) {
                searchAndUpdate(ref, target);
            }
            // 通知最近的节点更新路由表
            target.updateNeighbour();
        }
        nodesInNetwork.add(target);
    }

    private void searchAndUpdate(Node2D reference, Node2D target) {
        Node2D previous = null;
        while(previous != reference) {
            previous = reference;
            Collection<Node2D> candidates = previous.queryNearestNodes(target);
            target.updateContacts(candidates);
            reference = target.getNearestNode();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Network2D");
        sb.append("\r\n");
        nodesInNetwork.forEach(n->sb.append("  ").append(n).append("\r\n"));
        return sb.toString();
    }
}
