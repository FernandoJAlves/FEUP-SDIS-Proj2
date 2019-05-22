package dbs.chord.messages;

import java.io.Serializable;

/**
 * Base class of all Chord messages.
 *
 * Messages have an underlying 'key' object, on which Observers may register in
 * ChordDispatcher. When a message arrives which matches the given 'key', the
 * Observer is notified of this message.
 */
public abstract class ChordMessage implements Serializable {

    private final ChordMessageKey key;

    public ChordMessage(String kind) {
        this.key = new ChordMessageKey(kind);
    }

    public ChordMessage(ChordMessageKey key) {
        assert key != null;
        this.key = key;
    }

    public ChordMessageKey getKey() {
        return key;
    }

    public String getKind() {
        return key.getKind();
    }
}
