package p2p.research.nezzar.kad2;

import android.os.*;
import android.util.Log;
import p2p.research.nezzar.Network;

import java.util.*;
import java.util.function.BinaryOperator;

public class NetworkKad2 extends Network {

    private static final String TAG = "KAD2";

    public List<KadNode2> nodesInNetwork = new ArrayList<>();
    private int index = 0;
    private int refBitIndex = -1;

    final int HELLO = 0;
    final int FINISH = -1;
    final int CONSTRUCT = 1;
    /**
     * Message adjustMsg = kh.obtainMessage(
     *     what:ADJUST,
     *     arg1:reference.node.index,
     *     arg2:bitIndex
     * );
     */
    final int ADJUST = 2;
    /**
     * kh.obtainMessage(
     *      what:CONNECT_AND_QUERY,
     *      arg1:target.node.index,
     *      arg2:nextContact.node.index
     *      );
     */
    final int CONNECT_AND_QUERY = 3;

    /**
     * 广播新节点
     * kh.obtainMessage(
     *     what:BROADCAST_NEW_NODE,
     *     arg1:target.node.index,
     *     arg2:reference.node.index
     *     );
     */
    final int BROADCAST_NEW_NODE = 4;
    /**
     * 发起广播新节点
     * kh.obtainMessage(
     *     what:LAUNCH_BROADCAST,
     *     arg1:target.node.index,
     *     arg2:reference.node.index
     *     );
     */
    final int LAUNCH_BROADCAST = 5;

    private class KadHandler extends Handler {
        public KadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case HELLO:
                    Log.i(TAG, "Hello Kad Network. N = " + N + ", K = " + K);
                    break;
                case CONSTRUCT: {
                    if (index < nodes.length) {
                        refBitIndex = 256;
                        KadNode2 target = new KadNode2(nodes[index], NetworkKad2.this);
                        construct(target);
                        index++;
                    }
                }
                break;
                case ADJUST: {
                    // 调整网络结构
                    KadNode2 target = nodesInNetwork.get(msg.arg1);
                    int bitIndex = msg.arg2;
                    adjust(target, bitIndex);
                }
                break;
                case CONNECT_AND_QUERY: {
                    KadNode2 target = nodesInNetwork.get(msg.arg1);
                    KadNode2 reference = nodesInNetwork.get(msg.arg2);
                    connectAndQuery(target, reference);
                }
                break;
                case LAUNCH_BROADCAST: {
                    KadNode2 target = nodesInNetwork.get(msg.arg1);
                    KadNode2 reference = nodesInNetwork.get(msg.arg2);
                    launchBroadcast(target, reference);
                }
                break;
                case BROADCAST_NEW_NODE: {
                    KadNode2 target = nodesInNetwork.get(msg.arg1);
                    KadNode2 reference = nodesInNetwork.get(msg.arg2);
                    checkAndBroadcast(target, reference);
                }
                break;
                case FINISH: {
                    Log.i(TAG, "Kad Network finished constructing.");
                    getLooper().quit();
                }
                break;
            }
        }
    }

    KadHandler kh = null;

    public NetworkKad2(int n, int k) {
        super(n, k);
    }

    public NetworkKad2(int n, int k, Iterator<String> itr) {
        super(n, k, itr);
    }

    /**
     * 构建网络。只执行一次。第二次执行无动作。
     * @return
     */
    public boolean construct() {
        // construct the network
        if (index >= N) {
            return false;
        }

        HandlerThread ht = new HandlerThread("Constructing KAD2 network");
        ht.start();

        kh = new KadHandler(ht.getLooper());

        kh.sendEmptyMessage(HELLO);
        for (int i = 0; i <N ; i++) {
            kh.sendEmptyMessage(CONSTRUCT);
            //SystemClock.sleep(50);
        }
        kh.sendEmptyMessage(FINISH);

        try {
            ht.join();
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }

        kh = null;

        return true;
    }

    private void construct(KadNode2 target) {
        Log.i(TAG, "Constructing " + target);
        if (!nodesInNetwork.isEmpty()) {
            KadNode2 reference = nodesInNetwork.get(0);
            connectAndQuery(target, reference);
        }
        nodesInNetwork.add(target);
    }

    /**
     * 以 reference 为起点，查询 target 的联系人
     * @param target 查询目标
     * @param reference 参照起点
     */
    private void connectAndQuery(KadNode2 target, KadNode2 reference) {
        refBitIndex--;
        if (refBitIndex < 0 || refBitIndex > 255) {
            return;
        }
        KadNode2 nextContact = reference.getNextContact(target, refBitIndex);
        // 4.发起下一个bit位的查询
        if (nextContact != null) {
            reference = nextContact;
        }
        Message m1 = kh.obtainMessage(
                CONNECT_AND_QUERY,
                target.node.index,
                reference.node.index
        );
        kh.sendMessageAtFrontOfQueue(m1);

        if (nextContact != null) {
            Log.i(TAG, "  connect " + target.node.index + " and " + nextContact.node.index + " on " + refBitIndex);
            target.addAt(refBitIndex, nextContact);
            nextContact.addAt(refBitIndex, target);

            // 1.调整 target
            // 2.调整 nextContact
            // 3.发起新节点广播

            // 使用 send message at front of queue，添加顺序与执行顺序相反
            // 3.发起新节点广播
            Message launchBroadcast = kh.obtainMessage(
                    LAUNCH_BROADCAST,
                    target.node.index,
                    nextContact.node.index
            );
            kh.sendMessageAtFrontOfQueue(launchBroadcast);
            // 2.调整 nextContact
            scheduleToAdjust(nextContact, refBitIndex);
            // 1.调整 target
            scheduleToAdjust(target, refBitIndex);
        }

    }

    /**
     * 检查在 bitIndex 位的联系人数量有没有超量。如果超量则做出调整。
     * 当某个联系人列表超员时，将执行选择性断开。断开时提供备用联系人给对方。
     * 选择性断开时，根据下述4个原则来确定被断开的节点。
     * 原则一：保留一个 在线时间最长的节点。
     * 原则二：保留一个 ping 值最低的节点。
     * 原则三：切断一个当前 bit 位列表联系人数量大于1，且XOR距离最远的节点。
     * 原则四：如果所有的联系人都在当前 bit 位上缺少联系人，切断XOR距离最远的节点，然后推荐若干个联系人节点给对方。
     * 提供备用联系人时，先寻找相对于对方的距离与自己在同一个bit位联系人列表的节点(优先选择bit位上的联系人未满的节点)。
     * 如果这样的节点不存在，则寻找一个与自己的 XOR 距离小于自己与对方距离的节点。(因为对方是XOR最远的节点，因此一定存在一个这样的节点)
     * @param target 目标对象
     * @param bitIndex 对应的bit索引
     */
    private void adjust(KadNode2 target, int bitIndex) {
        if (target.sizeAt(bitIndex) > K) {
            List<KadNode2> disconnectRange = new ArrayList<>();
            disconnectRange.addAll(target.dataAt(bitIndex));
            Log.i(TAG, "  adjust " + target.node.index + " at " + bitIndex);
            // 原则一：保留一个 在线时间最长的节点。
            KadNode2 longTermContact = disconnectRange.stream().reduce((a, b)->a.node.index < b.node.index ? a : b).get();
            disconnectRange.remove(longTermContact);
            // 原则二：保留一个 ping 值最低的节点。
            KadNode2 lowPingContact = disconnectRange.stream().reduce((a, b)->
                    getPingDistance(target.node.index, a.node.index)
                            < getPingDistance(target.node.index, b.node.index) ? a : b).get();
            disconnectRange.remove(lowPingContact);
            if (disconnectRange.size() < 2) {
                throw new RuntimeException("K should be greater than or equal to 3");
            }
            // 原则三：切断一个当前 bit 位列表联系人数量大于1，且地址距离最远的节点。
            BinaryOperator<KadNode2> farther = (a, b)->{
                return target.getAbsoluteDistanceValueFrom(a).compareTo(
                        target.getAbsoluteDistanceValueFrom(b)) < 0 ? b : a;
            };
            KadNode2 toBeDisconnected = disconnectRange.stream().filter(n->n.sizeAt(bitIndex) > 1)
                    .reduce(farther).orElse(null);
            if (toBeDisconnected != null) {
                Log.i(TAG, "  disconnect " + target.node.index + " and " + toBeDisconnected.node.index);
                target.removeAt(bitIndex, toBeDisconnected);
                toBeDisconnected.removeAt(bitIndex, target);
            } else {
                // 原则四：如果所有的联系人都在当前 bit 位上缺少联系人，切断地址距离最远的节点，
                toBeDisconnected = disconnectRange.stream().reduce(farther).get();
                target.removeAt(bitIndex, toBeDisconnected);
                toBeDisconnected.removeAt(bitIndex, target);
                Log.i(TAG, "  disconnect " + target.node.index + " and " + toBeDisconnected.node.index);
                // 然后推荐若干个联系人节点给对方。
                List<KadNode2> suggestingRange = new ArrayList<>();
                // 提供备用联系人时，先寻找相对于对方的距离与自己在同一个bit位联系人列表的节点。
                for (int i = 0; i < bitIndex; i++) {
                    suggestingRange.addAll(target.dataAt(i));
                }
                // 且必须选择bit位上的联系人未满的节点
                KadNode2 suggestedContact = suggestingRange.stream().filter(n->n.sizeAt(bitIndex) < K)
                        .findAny().orElse(null);
                if (suggestedContact != null) {
                    toBeDisconnected.addAt(bitIndex, suggestedContact);
                    suggestedContact.addAt(bitIndex, toBeDisconnected);
                    Log.i(TAG, "  arrange connect " + toBeDisconnected.node.index + " and " + suggestedContact.node.index +" at(good) " + bitIndex);
                } else {
                    // 如果这样的节点不存在，则在自己当前 bit 位列表联系人寻找一个与自己的地址距离(*)小于自己与对方距离的节点。
                    // (因为对方是地址距离(*)最远的节点，因此一定存在一个这样的节点，且任意一个皆是)
                    disconnectRange.remove(toBeDisconnected);
                    suggestedContact = disconnectRange.get(0);
                    // 将被切断的节点与推荐的节点连接起来，并再次调整网络
                    int anotherBitIndex = KadNode2.getBitIndex(toBeDisconnected, suggestedContact);
                    if (anotherBitIndex < 0) {
                        throw new RuntimeException("design issue");
                    }
                    toBeDisconnected.addAt(anotherBitIndex, suggestedContact);
                    suggestedContact.addAt(anotherBitIndex, toBeDisconnected);
                    Log.i(TAG, "  arrange connect " + toBeDisconnected.node.index + " and " + suggestedContact.node.index + " on " + anotherBitIndex);
                    // 1.调整 toBeDisconnected
                    // 2.调整 suggestedContact
                    scheduleToAdjust(toBeDisconnected, anotherBitIndex);
                    // 1.调整 suggestedContact
                    scheduleToAdjust(suggestedContact, anotherBitIndex);
                }
            }
        }
    }

    private void scheduleToAdjust(KadNode2 target, int bitIndex) {
        Message adjustSuggestedContact = kh.obtainMessage(
                ADJUST,
                target.node.index,
                bitIndex);
        kh.sendMessageAtFrontOfQueue(adjustSuggestedContact);
    }

    /**
     * 1) 收到重复的广播消息，直接抛弃
     * 2) 判断周围的所有节点与新节点的连接必要性，并广播推送新节点。
     */
    public void launchBroadcast(KadNode2 target, KadNode2 reference) {
        if (reference.isAlreadyKnown(target)) {
            return;
        }
        reference.getAllContactsWithStream().forEach(contact -> {
            int bitIndex = KadNode2.getBitIndex(contact, target);
            if (bitIndex < 0) {
                return;
            }
            if (contact.isEmptyAt(bitIndex)) {
                Message broadcastNewNode = kh.obtainMessage(
                        BROADCAST_NEW_NODE,
                        target.node.index,
                        contact.node.index
                );
                kh.sendMessageAtFrontOfQueue(broadcastNewNode);
            }
        });
    }

    /**
     * 1) 收到重复的广播消息，直接抛弃
     * 2) 收到广播的节点，如果在与新节点的关联bit位不为空，则直接抛弃消息。
     * 3) 与新节点互相添加联系人
     * 4) 广播新节点给周围的节点
     * 5) 如果互相添加联系人时任意节点的联系人满员，则根据选择性断开原则调整网络。
     */
    public void checkAndBroadcast(KadNode2 target, KadNode2 reference) {
        // 1) 收到重复的广播消息，直接抛弃
        if (reference.isAlreadyKnown(target)) {
            return;
        }
        // 2) 收到广播的节点，如果在与新节点的关联bit位不为空，则直接抛弃消息。
        int bitIndex = KadNode2.getBitIndex(target, reference);
        if (!reference.isEmptyAt(bitIndex)) {
            return;
        }
        // 3) 与新节点互相添加联系人
        reference.addAt(bitIndex, target);
        target.addAt(bitIndex, reference);
        Log.i(TAG, "  broad cast received and connect " + reference.node.index + " and " + target.node.index +" at " + bitIndex);
        // 4) 广播新节点给周围的节点
        reference.getAllContactsWithStream().forEach(contact -> {
            int anotherBitIndex = KadNode2.getBitIndex(contact, target);
            if (anotherBitIndex < 0) {
                return;
            }
            if (contact.isEmptyAt(anotherBitIndex)) {
                Message broadcastNewNode = kh.obtainMessage(
                        BROADCAST_NEW_NODE,
                        target.node.index,
                        contact.node.index
                );
                kh.sendMessageAtFrontOfQueue(broadcastNewNode);
            }
        });
        // 5) 如果互相添加联系人时任意节点的联系人满员，则根据选择性断开原则调整网络。
        scheduleToAdjust(target, bitIndex);
    }

    public static class Report {
        int tryCount;
        int successCount;
        int jumpCount;
        long pingSum;
        Report() {
            tryCount = 1;
            successCount = 0;
            jumpCount = 0;
            pingSum = 0;
        }
        static Report combine(Report a, Report b) {
            if (a == null) {
                return b;
            } else if (b == null) {
                return a;
            }
            Report r = new Report();
            r.tryCount = a.tryCount + b.tryCount;
            r.successCount = a.successCount + b.successCount;
            r.jumpCount = a.jumpCount + b.jumpCount;
            r.pingSum = a.pingSum + b.pingSum;
            return r;
        }

        @Override
        public String toString() {
            return "Report{" +
                    "tryCount=" + tryCount +
                    ", successCount=" + successCount +
                    ", jumpCount=" + jumpCount +
                    ", pingSum=" + pingSum +
                    ", successRate=" + (successCount * 100.0 / tryCount) +
                    ", jumpAverage=" + (jumpCount * 1.0 / tryCount) +
                    ", pingAverage=" + (pingSum * 1.0 / tryCount) +
                    '}';
        }
    }

    public Report verifyFull() {
        Report total = null;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i == j) {
                    continue;
                }
                Report r = verify(i, j);
                total = Report.combine(total, r);
            }
        }
        return total;
    }

    public Report verifyRandom(int c) {
        Report total = null;
        Random rnd = new Random();
        for (int i = 0; i < c; i++) {
            int from = rnd.nextInt(N);
            int to = rnd.nextInt(N);
            while (from == to) {
                to = rnd.nextInt(N);
            }
            Report r = verify(from, to);
            total = Report.combine(total, r);
        }
        return total;
    }

    private Report verify(int from, int to) {
        Log.d("", "routing from " + from + " to " + to);
        KadNode2 current = nodesInNetwork.get(from);
        KadNode2 destination = nodesInNetwork.get(to);

        Report report = new Report();

        for (int i = 0; i < 100; i++) {
            if (current.equals(destination)) {
                report.successCount = 1;
                return report;
            }
            KadNode2 next = current.getNextJump(destination.getBits());
            Log.d("", "  next jump to " + (next == null ? "" : String.valueOf(next.node.index)));
            if (next == null) {
                break;
            }
            report.jumpCount++;
            report.pingSum += getPingDistance(current.node.index, next.node.index);
            current = next;
        }
        Log.w("", "WARNING!!! bad routing from " + from + " to " + to);
        return new Report();
    }
}
