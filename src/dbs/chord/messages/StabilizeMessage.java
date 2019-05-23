package dbs.chord.messages;

public final class StabilizeMessage extends ChordMessage {

    public StabilizeMessage() {
        super(new ChordMessageKey("STABILIZE"));
    }

    @Override
    public String toString() {
        return "STABILIZE(" + getKey() + ")";
    }
}
