package p2p.research;

import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import p2p.research.nezzar.Network;
import p2p.research.nezzar.kad.KadNode;
import p2p.research.nezzar.kad.NetworkKad;
import p2p.research.nezzar.n2d.Network2D;
import p2p.research.nezzar.n2d.Node2D;
import p2p.research.ui.SWTUI;

import java.math.BigInteger;
import java.util.BitSet;

/**
 * Hello world!
 *
 */
public class App {
    private static final String TAG = "App";

    private static Object lock;

    public static void main( String[] args ) {
        Log.i(TAG, "Hello World! current time : " + SystemClock.uptimeMillis());

        //demonstrateHandlerUsage();
        //runSomething();
        checkKad();

        Log.i(TAG, "See you! current time : " + SystemClock.uptimeMillis());
    }

    public static void checkKad() {
        int n = 10;
        int k = 3;
        NetworkKad network = new NetworkKad(n, k);
        long t1 = SystemClock.uptimeMillis();
        Log.i(TAG, "Basic info initialized : " + t1);
        network.construct();
        Log.i(TAG, "Basic info initialized at cost of : " + t1);
        NetworkKad.Report report = network.verifyFull();
        Log.i(TAG, report.toString());
        showUIofNetwork(network);
    }

    private static void showUIofNetwork(NetworkKad network) {
        SWTUI ui = new SWTUI(1024, 900);
        ui.setPaintListener(e->{
            for (int i = 0; i < network.nodesInNetwork.size(); i++) {
                KadNode node1 = network.nodesInNetwork.get(i);
                Point p = getPoint(1024, 900, i, network.nodesInNetwork.size());
                Color defaultBg = e.gc.getBackground();
                e.gc.setBackground(new Color(ui.display, 0, 0, 192));
                e.gc.fillOval(p.x-10, p.y-10, 20, 20);
                e.gc.setBackground(defaultBg);
                String name = String.valueOf(i);
                e.gc.drawText(name, p.x + 20, p.y + 20);
                for (int j = 0; j < i; j++) {
                    KadNode node2 = network.nodesInNetwork.get(j);
                    int bitIndex = KadNode.getBitIndex(node1, node2);
                    if (bitIndex >= 0 && node1.contacts.get(bitIndex).contains(node2)) {
                        Point p2 = getPoint(1024, 900, j, network.nodesInNetwork.size());
                        e.gc.drawLine(p.x, p.y, p2.x, p2.y);
                    }
                }
            }
        });

        ui.show();
    }

    private static Point getPoint(int width, int height, int i, int n) {
        double angle = Math.PI * 2 * i / n;
        int r = (int)(Math.min(width, height) * 0.4);
        int x = width / 2 + (int) (r * Math.sin(angle));
        int y = height / 2 - (int) (r * Math.cos(angle)) - 40;
        return new Point(x, y);
    }

    public static void showUIofNetwork2D() {
        Network2D n2d = new Network2D(8, 3);

        lock = new Object();

        // TODO 绘制连接图
        SWTUI ui = new SWTUI(1024, 900);
        int x = 50;
        int y = 10;
        int w = 800;
        int h = 800;

        ui.setPaintListener(e->{
            synchronized (lock) {
                e.gc.drawRectangle(x,y,w,h);
                for (Node2D node2D : n2d.nodesInNetwork) {
                    Point p = translate(node2D, x, y, w, h);
                    Color defaultBg = e.gc.getBackground();
                    e.gc.setBackground(new Color(ui.display, 0, 0, 192));
                    e.gc.fillOval(p.x, p.y, 10, 10);
                    e.gc.setBackground(defaultBg);
                    String name = String.valueOf(node2D.thisNode.index);
                    e.gc.drawText(name, p.x + 20, p.y);
                }
                Color defaultFg = e.gc.getForeground();
                Color red = new Color(ui.display, 255, 0, 0);
                Color purple = new Color(ui.display, 255, 0, 255);
                for (int i = 0; i < n2d.nodesInNetwork.size() ; i++) {
                    for (int j = i + 1; j < n2d.nodesInNetwork.size() ; j++) {
                        boolean connected = n2d.isConnected(i, j) || n2d.isConnected(j, i);
                        boolean pingConnected = n2d.isPingConnected(i, j) || n2d.isPingConnected(j, i);
                        boolean both = connected && pingConnected;
                        Color pen = both ? purple : pingConnected ? red : defaultFg;
                        if (connected/* || pingConnected */) {
                            Point from = translate(n2d.nodesInNetwork.get(i), x, y, w, h);
                            Point to = translate(n2d.nodesInNetwork.get(j), x, y, w, h);
                            e.gc.setForeground(pen);
                            e.gc.drawLine(from.x, from.y, to.x, to.y);
                        }
                    }
                }
                e.gc.setForeground(defaultFg);
            }
        });

        Thread t2 = new Thread(()->{
            //SystemClock.sleep(1000);
            while (true) {
                synchronized (lock) {
                    if (!n2d.construct()) {
                        break;
                    }
                }
                ui.refresh();
                //SystemClock.sleep(500);//TODO ConcurrentModificationException
            }
            ui.refresh();

            System.out.println(n2d);
        });
        t2.start();

        ui.show();
        // TODO 评估连通性，通讯代价

    }

    static Point translate(Node2D node, int xx, int yy, int w, int h) {
        int x = xx + node.getShortX() * w / Node2D.getShortWidth();
        int y = yy + (Node2D.getShortWidth() - node.getShortY()) * h / Node2D.getShortWidth();
        Point p = new Point(x, y);
        return p;
    }

    public static void demonstrateHandlerUsage() {
        HandlerThread ht = new HandlerThread("Handler Thread");
        ht.start();

        AppHandler ah = new AppHandler(ht.getLooper());

        ah.sendEmptyMessage(AppHandler.HELLO);
        ah.sendEmptyMessage(AppHandler.BYE);
        ah.sendEmptyMessage(AppHandler.QUIT);

        try {
            ht.join();
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }

    }

    public static void runSomething() {
//        byte[] data = new byte[2];
//        data[0] = (byte) 0x7F;
//        data[1] = (byte) 0xFF;
//        BigInteger bi = new BigInteger(data);
//        System.out.println(bi);
        byte[] data = new byte[33];
        data[0] = 1;
        BigInteger _2pow256 = new BigInteger(data);
        System.out.println(_2pow256);
        data[0] = 0;
        System.out.println(_2pow256);
    }
}
