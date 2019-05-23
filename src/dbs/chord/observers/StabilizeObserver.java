package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.messages.StabilizeMessage;

public class StabilizeObserver extends ChordObserver {

    public StabilizeObserver() {
        super(new ChordMessageKey("STABILIZE"));
    }

    @Override
    public boolean notify(ChordMessage message) {
        if (!(message instanceof StabilizeMessage)) {
            System.err.println("StabilizeObserver received message not of type StabilizeMessage");
            return false;
        }
        Node.get().handleStabilize((StabilizeMessage) message);
        return false;
    }

}
