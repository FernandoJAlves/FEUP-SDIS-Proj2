package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.messages.PredecessorMessage;

public class PredecessorObserver extends PermanentObserver {

    public PredecessorObserver() {
        super(new ChordMessageKey("PREDECESSOR"));
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof PredecessorMessage;
        Node.get().handlePredecessor((PredecessorMessage) message);
    }

    @Override
    public String toString() {
        return "PredecessorObserver";
    }
}
