package io.realm.examples.arm64stress;

import android.os.SystemClock;

/**
 * Simple class for making sure that UI thread and background thread is making progress
 */
public class WatchDog {

    private Thread watchdog;
    private Watchable watchable;
    private transient long lastUIProgress;
    private transient long lastBackgroundProgress;
    private long sleepMs = 1000;
    private transient boolean isStarted;

    public void start() {
        watchdog = new Thread(new Runnable() {
            @Override
            public void run() {
                while(isStarted) {
                    SystemClock.sleep(sleepMs);
                    long newUIProgress = watchable.getUIProgress();
                    long newBackgroundProgress = watchable.getBackgroundProgress();

// TODO: Hmm, the UI seems to be lagging behind sometimes :(
//                    String state = String.format("UI: %s -> %s, Background: %s-> %s", lastUIProgress, newUIProgress, lastBackgroundProgress, newBackgroundProgress);
//                    if (newBackgroundProgress <= lastBackgroundProgress) {
//                        throw new IllegalStateException("Background thread seems to be hanging! " + state);
//                    }
//                    if (newUIProgress <= lastUIProgress) {
//                        throw new IllegalStateException("UI thread seems to be hanging! " + state);
//                    }
                    lastUIProgress = newUIProgress;
                    lastBackgroundProgress = newBackgroundProgress;
                    watchable.notifyUpdate(lastUIProgress, lastBackgroundProgress);
                }
            }
        });
        isStarted = true;
        watchdog.start();
    }

    public void setWatchable(Watchable watchable) {
        this.watchable = watchable;
        lastUIProgress = watchable.getUIProgress();
        lastBackgroundProgress = watchable.getBackgroundProgress();
    }

    public interface Watchable {
        long getUIProgress();
        long getBackgroundProgress();
        void notifyUpdate(long lastUIProgress, long lastBackgroundProgress);
    }
}
