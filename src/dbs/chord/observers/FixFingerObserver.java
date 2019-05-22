package dbs.chord.observers;

import java.math.BigInteger;

import dbs.chord.Node;
import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.Responsible;

/**
 * One-time observer
 *
 * Used when the Node's FixFinger runnable calls fixFinger() on a given index.
 */
public class FixFingerObserver extends ChordObserver {

    private final int fingerIndex;

    public FixFingerObserver(BigInteger chordId, int fingerIndex) {
        super(new ChordIdKey("RESPONSIBLE", chordId));
        this.fingerIndex = fingerIndex;
    }

    @Override
    public boolean notify(ChordMessage message) {
        if (!(message instanceof Responsible))
            return false;
        Node.get().handleFixFinger((Responsible) message, fingerIndex);
        return true;
    }
}
