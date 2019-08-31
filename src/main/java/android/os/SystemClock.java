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


/**
 * Core timekeeping facilities.
 *
 * <p> Three different clocks are available, and they should not be confused:
 *
 *
 * There are several mechanisms for controlling the timing of events:
 *
 * <ul>
 *     <li> <p> Standard functions like {@link Thread#sleep(long)
 *     Thread.sleep(millis)} and {@link Object#wait(long) Object.wait(millis)}
 *     are always available.  These functions use the {@link #uptimeMillis}
 *     clock; if the device enters sleep, the remainder of the time will be
 *     postponed until the device wakes up.  These synchronous functions may
 *     be interrupted with {@link Thread#interrupt Thread.interrupt()}, and
 *     you must handle {@link InterruptedException}.
 *
 *     <li> <p> {@link #sleep SystemClock.sleep(millis)} is a utility function
 *     very similar to {@link Thread#sleep(long) Thread.sleep(millis)}, but it
 *     ignores {@link InterruptedException}.  Use this function for delays if
 *     you do not use {@link Thread#interrupt Thread.interrupt()}, as it will
 *     preserve the interrupted state of the thread.
 *
 *     <li> <p> The {@link android.os.Handler} class can schedule asynchronous
 *     callbacks at an absolute or relative time.  Handler objects also use the
 *     {@link #uptimeMillis} clock, and require an {@link android.os.Looper
 *     event loop} (normally present in any GUI application).
 *
 * </ul>
 */
public final class SystemClock {
    /**
     * This class is uninstantiable.
     */
    private SystemClock() {
        // This space intentionally left blank.
    }

    private static final long BOOT_TIME = System.currentTimeMillis();

    /**
     * Waits a given number of milliseconds (of uptimeMillis) before returning.
     * Similar to {@link java.lang.Thread#sleep(long)}, but does not throw
     * {@link InterruptedException}; {@link Thread#interrupt()} events are
     * deferred until the next interruptible operation.  Does not return until
     * at least the specified number of milliseconds has elapsed.
     *
     * @param ms to sleep before returning, in milliseconds of uptime.
     */
    public static void sleep(long ms)
    {
        long start = uptimeMillis();
        long duration = ms;
        boolean interrupted = false;
        do {
            try {
                Thread.sleep(duration);
            }
            catch (InterruptedException e) {
                interrupted = true;
            }
            duration = start + ms - uptimeMillis();
        } while (duration > 0);

        if (interrupted) {
            // Important: we don't want to quietly eat an interrupt() event,
            // so we make sure to re-interrupt the thread so that the next
            // call to Thread.sleep() or Object.wait() will be interrupted.
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns milliseconds since boot, not counting time spent in deep sleep.
     * <b>Note:</b> This value may get reset occasionally (before it would
     * otherwise wrap around).
     *
     * @return milliseconds of non-sleep uptime since boot.
     */
    //native public static long uptimeMillis();
    public static long uptimeMillis() {
        return System.currentTimeMillis() - BOOT_TIME;
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

}

