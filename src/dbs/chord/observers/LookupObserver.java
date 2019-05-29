package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.LookupMessage;

/**
 * Bread-and-butter observer that receives lookup requests (successor requests).
 */
public final class LookupObserver extends PermanentObserver {

    public LookupObserver() {
        super("LOOKUP");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof LookupMessage;
        Node.get().handleLookup((LookupMessage) message);
    }

    @Override
    public String toString() {
        return "LookupObserver";
    }
}
