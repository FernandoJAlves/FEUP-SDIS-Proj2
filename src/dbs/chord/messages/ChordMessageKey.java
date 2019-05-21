package dbs.chord.messages;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

/**
 * The key of a ChordMessage consists of a simple text string and a Chord id.
 * The chordid may be null if it does not apply for that kind of message.
 */
public final class ChordMessageKey implements Serializable {

    private final String kind;
    private final BigInteger chordid;

    public ChordMessageKey(String kind, BigInteger chordid) {
        assert kind != null;
        this.kind = kind;
        this.chordid = chordid;
    }

    public String getKind() {
        return kind;
    }

    public BigInteger getId() {
        return chordid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chordid, kind);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ChordMessageKey))
            return false;
        ChordMessageKey other = (ChordMessageKey) obj;
        return Objects.equals(chordid, other.chordid) && Objects.equals(kind, other.kind);
    }
}
