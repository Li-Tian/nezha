package p2p.research;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class AppHandler extends Handler {

    public static final int QUIT = -1;

    public static final int HELLO = 1;

    public static final int BYE = 2;

    private static final String TAG = "AppHandler";

    public AppHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
        switch(msg.what) {
            case HELLO:
                Log.i(TAG, "Wake up. Neo.");
                break;
            case BYE:
                Log.i(TAG, "Follow the white rabbit.");
                break;
            case QUIT:
                Log.i(TAG, "Looper quit.");
                getLooper().quit();
                break;
        }
    }
}
