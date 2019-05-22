package dbs.chord.observers;

import java.math.BigInteger;

import dbs.chord.Node;
import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ResponsibleMessage;

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
        if (!(message instanceof ResponsibleMessage)) {
            System.err.println("FixFingerObserver received message not of type ResponsibleMessage");
            return false;
        }
        Node.get().handleFixFingerResponse((ResponsibleMessage) message, fingerIndex);
        return true;
    }
}
