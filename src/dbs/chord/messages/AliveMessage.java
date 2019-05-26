package dbs.chord.messages;

public class AliveMessage extends ChordMessage {

    public AliveMessage() {
        super(new ChordMessageKey("ISALIVE"));
    }

    @Override
    public String toString() {
        return "ISALIVE";
    }
}
