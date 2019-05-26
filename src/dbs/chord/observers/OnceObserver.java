package dbs.chord.observers;

import dbs.chord.messages.ChordMessageKey;

public abstract class OnceObserver extends ChordObserver {

    public OnceObserver(String kind) {
        super(kind);
    }

    public OnceObserver(ChordMessageKey key) {
        super(key);
    }
}
