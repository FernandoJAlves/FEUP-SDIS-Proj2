package dbs.chord.messages;

public final class StabilizeMessage extends ChordMessage {

    public StabilizeMessage() {
        super(new ChordMessageKey("STABILIZE"));
    }
}