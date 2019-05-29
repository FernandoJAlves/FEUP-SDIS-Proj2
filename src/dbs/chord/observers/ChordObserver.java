package dbs.chord.observers;

import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;

public abstract class ChordObserver {

    private final ChordMessageKey key;

    protected ChordObserver(String kind) {
        this.key = new ChordMessageKey(kind);
    }

    protected ChordObserver(ChordMessageKey key) {
        this.key = key;
    }

    /**
     * To be run when this observer's subscribed message has arrived,
     * and he has been removed from the Observer set.
     *
     * @param message The message received that had been subscribed to.
     */
    public abstract void notify(ChordMessage message);

    /**
     * @return the key this observer is subscribed on.
     */
    public final ChordMessageKey getSubscribedKey() {
        return key;
    }
}
