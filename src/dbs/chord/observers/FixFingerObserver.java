package dbs.chord.observers;

import java.math.BigInteger;
import dbs.chord.Node;
import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ResponsibleMessage;

/**
 * Used when the Node's FixFinger runnable calls fixFinger() on a given index.
 * These are actually subscribed permanently because the chord id and finger index
 * does not change for each finger over time.
 */
public class FixFingerObserver extends PermanentObserver {

    private final int fingerIndex;

    public FixFingerObserver(BigInteger chordId, int fingerIndex) {
        super(new ChordIdKey("RESPONSIBLE", chordId));
        this.fingerIndex = fingerIndex;
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof ResponsibleMessage;
        Node.get().handleFixFingerResponse((ResponsibleMessage) message, fingerIndex);
    }

    @Override
    public String toString() {
        return "FixFingerObserver " + fingerIndex;
    }
}
