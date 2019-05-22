package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.messages.PredecessorMessage;

public class PredecessorObserver extends ChordObserver {

    public PredecessorObserver() {
        super(new ChordMessageKey("PREDECESSOR"));
    }

    @Override
    public boolean notify(ChordMessage message) {
        if (!(message instanceof PredecessorMessage)) {
            System.err.println("PredecessorObserver received a message not of type PredecessorMessage");
            return false;
        }
        Node.get().handlePredecessor((PredecessorMessage) message);
        return false;
    }

}
