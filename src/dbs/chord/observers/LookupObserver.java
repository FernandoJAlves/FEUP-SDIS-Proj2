package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.messages.Lookup;

/**
 * Permanent observer.
 *
 * Bread-and-butter observer that receives lookup requests (successor requests).
 */
public final class LookupObserver extends ChordObserver {

    public LookupObserver() {
        super(new ChordMessageKey("LOOKUP"));
    }

    @Override
    public boolean notify(ChordMessage message) {
        if (!(message instanceof Lookup))
            return false;
        Node.get().handleLookup((Lookup) message);
        return false;
    }
}
