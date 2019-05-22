package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.messages.LookupMessage;

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
        if (!(message instanceof LookupMessage)) {
            System.err.println("LookupObserver received message not of type LookupMessage");
            return false;
        }
        Node.get().handleLookup((LookupMessage) message);
        return false;
    }
}
