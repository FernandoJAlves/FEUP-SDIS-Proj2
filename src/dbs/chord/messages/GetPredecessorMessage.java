package dbs.chord.messages;

public final class GetPredecessorMessage extends ChordMessage {

    public GetPredecessorMessage() {
        super("GETPREDECESSOR");
    }

    @Override
    public String toString() {
        return "GETPREDECESSOR";
    }
}
