package dbs.chord.observers;

import dbs.chord.messages.ChordMessageKey;

public abstract class PermanentObserver extends ChordObserver {

    public PermanentObserver(String kind) {
        super(kind);
    }

    public PermanentObserver(ChordMessageKey key) {
        super(key);
    }
}
