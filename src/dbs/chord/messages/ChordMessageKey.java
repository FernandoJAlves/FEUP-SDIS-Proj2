package dbs.chord.messages;

import java.io.Serializable;
import java.util.Objects;

/**
 * The key of a ChordMessage consists of a simple text string and a Chord id.
 */
public class ChordMessageKey implements Serializable {

    private final String kind;

    public ChordMessageKey(String kind) {
        assert kind != null;
        this.kind = kind;
    }

    public String getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind);
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
        return Objects.equals(kind, other.kind);
    }
}
