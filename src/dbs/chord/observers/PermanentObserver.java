package dbs.chord.observers;

import dbs.chord.messages.ChordMessageKey;

public abstract class PermanentObserver extends ChordObserver {

    protected PermanentObserver(String kind) {
        super(kind);
    }

    protected PermanentObserver(ChordMessageKey key) {
        super(key);
    }
}
