package dbs.chord.messages;

import java.math.BigInteger;
import java.util.Objects;

import dbs.chord.Chord;

public class ChordIdKey extends ChordMessageKey {

    private final BigInteger chordId;

    public ChordIdKey(String kind, BigInteger chordId) {
        super(kind);
        this.chordId = chordId;
    }

    public BigInteger getChordId() {
        return chordId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(chordId);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof ChordIdKey))
            return false;
        ChordIdKey other = (ChordIdKey) obj;
        return Objects.equals(chordId, other.chordId);
    }

    @Override
    public String toString() {
        return "message(" + getKind() + ", " + chordId + " " + Chord.percentStr(chordId) + ")";
    }
}
