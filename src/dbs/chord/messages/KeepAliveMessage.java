package dbs.chord.messages;

public class KeepAliveMessage extends ChordMessage {

    public KeepAliveMessage() {
        super("KEEPALIVE");
    }

    @Override
    public String toString() {
        return "KEEPALIVE";
    }
}
