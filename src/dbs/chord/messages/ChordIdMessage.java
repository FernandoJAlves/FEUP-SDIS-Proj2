package dbs.chord.messages;

import java.math.BigInteger;

public abstract class ChordIdMessage extends ChordMessage {

    public ChordIdMessage(String kind, BigInteger chordId) {
        super(new ChordIdKey(kind, chordId));
    }

    public BigInteger getChordId() {
        return ((ChordIdKey) getKey()).getChordId();
    }
}
