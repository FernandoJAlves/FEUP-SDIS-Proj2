package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.StabilizeMessage;

public class StabilizeObserver extends PermanentObserver {

    public StabilizeObserver() {
        super("STABILIZE");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof StabilizeMessage;
        Node.get().handleStabilize((StabilizeMessage) message);
    }

    @Override
    public String toString() {
        return "StabilizeObserver";
    }
}
