package p2p.research;

import android.os.HandlerThread;
import android.util.Log;
import p2p.research.nezzar.Network2D;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final String TAG = "App";
    public static void main( String[] args )
    {
        Log.i(TAG, "Hello World!" );
        demenstrate();
        Network2D n2d = new Network2D();

        Log.i(TAG, "See you!" );
    }

    public static void demenstrate() {
        HandlerThread ht = new HandlerThread("Handler Thread");
        ht.start();

        AppHandler ah = new AppHandler(ht.getLooper());

        ah.sendMessage(ah.obtainMessage(AppHandler.HELLO));
        ah.sendMessage(ah.obtainMessage(AppHandler.BYE));
        ah.sendMessage(ah.obtainMessage(AppHandler.QUIT));

        try {
            ht.join();
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }
    }
}
