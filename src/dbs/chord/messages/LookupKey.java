package dbs.chord.messages;

import java.math.BigInteger;
import java.util.Objects;

public class LookupKey extends ChordMessageKey {

    private final BigInteger chordId;

    public LookupKey(BigInteger chordId) {
        super("RESPONSIBLE");
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
        if (!(obj instanceof LookupKey))
            return false;
        LookupKey other = (LookupKey) obj;
        return Objects.equals(chordId, other.chordId);
    }

    @Override
    public String toString() {
        return "LookupKey (key " + chordId + ")";
    }
}
