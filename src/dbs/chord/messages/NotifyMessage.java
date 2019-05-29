package dbs.chord.messages;

public class NotifyMessage extends ChordMessage {

    public NotifyMessage() {
        super("NOTIFY");
    }

    @Override
    public String toString() {
        return "NOTIFY";
    }
}
