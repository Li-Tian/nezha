/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

import android.util.Log;

/**
 * Class used to run a message loop for a thread.  Threads by default do
 * not have a message loop associated with them; to create one, call
 * {@link #prepare} in the thread that is to run the loop, and then
 * {@link #loop} to have it process messages until the loop is stopped.
 *
 * <p>Most interaction with a message loop is through the
 * {@link Handler} class.
 *
 * <p>This is a typical example of the implementation of a Looper thread,
 * using the separation of {@link #prepare} and {@link #loop} to create an
 * initial Handler to communicate with the Looper.
 *
 * <pre>
 *  class LooperThread extends Thread {
 *      public Handler mHandler;
 *
 *      public void run() {
 *          Looper.prepare();
 *
 *          mHandler = new Handler() {
 *              public void handleMessage(Message msg) {
 *                  // process incoming messages here
 *              }
 *          };
 *
 *          Looper.loop();
 *      }
 *  }</pre>
 */
public class Looper {
    private static final String TAG = "Looper";

    // sThreadLocal.get() will return null unless you've called prepare().
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
    private static Looper sMainLooper;  // guarded by Looper.class

    final MessageQueue mQueue;
    final Thread mThread;
    volatile boolean mRun;

    /** Initialize the current thread as a looper.
     * This gives you a chance to create handlers that then reference
     * this looper, before actually starting the loop. Be sure to call
     * {@link #loop()} after calling this method, and end it by calling
     * {@link #quit()}.
     */
    public static void prepare() {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }

    /**
     * Initialize the current thread as a looper, marking it as an
     * application's main looper. The main looper for your application
     * is created by the Android environment, so you should never need
     * to call this function yourself.  See also: {@link #prepare()}
     */
    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }

    /** Returns the application's main looper, which lives in the main thread of the application.
     */
    public static Looper getMainLooper() {
        synchronized (Looper.class) {
            return sMainLooper;
        }
    }

    /**
     * Run the message queue in this thread. Be sure to call
     * {@link #quit()} to end the loop.
     */
    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        final MessageQueue queue = me.mQueue;

        for (;;) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, ">>>>> Dispatching to " + msg.target + " " +
                        msg.callback + ": " + msg.what);
            }

            msg.target.dispatchMessage(msg);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "<<<<< Finished to " + msg.target + " " + msg.callback);
            }

            msg.recycle();
        }
    }

    /**
     * Return the Looper object associated with the current thread.  Returns
     * null if the calling thread is not associated with a Looper.
     */
    public static Looper myLooper() {
        return sThreadLocal.get();
    }

    /**
     * Return the {@link MessageQueue} object associated with the current
     * thread.  This must be called from a thread running a Looper, or a
     * NullPointerException will be thrown.
     */
    public static MessageQueue myQueue() {
        return myLooper().mQueue;
    }

    private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mRun = true;
        mThread = Thread.currentThread();
    }

    /**
     * Quits the looper.
     *
     * Causes the {@link #loop} method to terminate as soon as possible.
     */
    public void quit() {
        mQueue.quit();
    }

    /**
     * Posts a synchronization barrier to the Looper's message queue.
     *
     * Message processing occurs as usual until the message queue encounters the
     * synchronization barrier that has been posted.  When the barrier is encountered,
     * later synchronous messages in the queue are stalled (prevented from being executed)
     * until the barrier is released by calling {@link #removeSyncBarrier} and specifying
     * the token that identifies the synchronization barrier.
     *
     * This method is used to immediately postpone execution of all subsequently posted
     * synchronous messages until a condition is met that releases the barrier.
     * Asynchronous messages (see {@link Message#isAsynchronous} are exempt from the barrier
     * and continue to be processed as usual.
     *
     * This call must be always matched by a call to {@link #removeSyncBarrier} with
     * the same token to ensure that the message queue resumes normal operation.
     * Otherwise the application will probably hang!
     *
     * @return A token that uniquely identifies the barrier.  This token must be
     * passed to {@link #removeSyncBarrier} to release the barrier.
     *
     * @hide
     */
    public final int postSyncBarrier() {
        return mQueue.enqueueSyncBarrier(SystemClock.uptimeMillis());
    }


    /**
     * Removes a synchronization barrier.
     *
     * @param token The synchronization barrier token that was returned by
     * {@link #postSyncBarrier}.
     *
     * @throws IllegalStateException if the barrier was not found.
     *
     * @hide
     */
    public final void removeSyncBarrier(int token) {
        mQueue.removeSyncBarrier(token);
    }

    /**
     * Return the Thread associated with this Looper.
     */
    public Thread getThread() {
        return mThread;
    }

    /** @hide */
    public MessageQueue getQueue() {
        return mQueue;
    }

    @Override
    public String toString() {
        return "Looper{" + Integer.toHexString(System.identityHashCode(this)) + "}";
    }

    public void reset() {
        mQueue.reset();
    }
}