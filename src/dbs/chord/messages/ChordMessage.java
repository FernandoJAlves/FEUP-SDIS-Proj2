package dbs.chord.messages;

import java.io.Serializable;
import java.util.Objects;
import dbs.chord.Node;
import dbs.chord.NodeInfo;

/**
 * Base class of all Chord messages.
 *
 * Messages have an underlying 'key' object, on which Observers may register in
 * ChordDispatcher. When a message arrives which matches the given 'key', the
 * Observer is notified of this message.
 */
public abstract class ChordMessage implements Serializable {

    private final ChordMessageKey key;
    private final NodeInfo sender = Node.get().getSelf();

    protected ChordMessage(ChordMessageKey key) {
        assert key != null && sender != null;
        this.key = key;
    }

    protected ChordMessage(String kind) {
        assert kind != null && sender != null;
        this.key = new ChordMessageKey(kind);
    }

    public ChordMessageKey getKey() {
        return key;
    }

    public String getKind() {
        return key.getKind();
    }

    public NodeInfo getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "MESSAGE(" + key + ", " + sender.shortStr() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, sender);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ChordMessage))
            return false;
        ChordMessage other = (ChordMessage) obj;
        return Objects.equals(key, other.key) && Objects.equals(sender, other.sender);
    }
}
