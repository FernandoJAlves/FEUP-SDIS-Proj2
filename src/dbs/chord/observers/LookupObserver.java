package dbs.chord.observers;

import dbs.chord.NodeLocalInfo;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;

public final class LookupObserver extends ChordObserver {

    public LookupObserver() {
        super(new ChordMessageKey("LOOKUP"));
    }

    @Override
    public boolean notify(ChordMessage message, NodeLocalInfo sender) {
        return false;
    }
}
