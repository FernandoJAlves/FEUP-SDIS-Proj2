package dbs.chord.observers;

import dbs.chord.messages.ChordMessageKey;

public abstract class OnceObserver extends ChordObserver {

    protected OnceObserver(String kind) {
        super(kind);
    }

    protected OnceObserver(ChordMessageKey key) {
        super(key);
    }
}
