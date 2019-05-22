package dbs.chord.messages;

public final class PredecessorUpdate extends ChordMessage {

    public PredecessorUpdate() {
        super(new ChordMessageKey("PREDECESSOR_UPDATE"));
    }
}
