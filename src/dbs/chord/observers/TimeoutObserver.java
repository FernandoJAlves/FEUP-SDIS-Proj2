package dbs.chord.observers;

import java.util.concurrent.TimeUnit;

import dbs.chord.messages.ChordMessageKey;

public abstract class TimeoutObserver extends ChordObserver {

    public static final TimeUnit unit = TimeUnit.MILLISECONDS;

    private final int delay;

    public TimeoutObserver(String kind, int delay) {
        super(kind);
        this.delay = delay;
    }

    public TimeoutObserver(ChordMessageKey key, int delay) {
        super(key);
        this.delay = delay;
    }

    /**
     * Called when the Dispatcher decides that this TimeoutObserver
     * has waited its maximum registered delay.
     */
    public abstract void timeout();

    /**
     * @return The initialized delay of this observer.
     */
    public int getDelay() {
        return delay;
    }
}
