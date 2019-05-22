package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.messages.PredecessorUpdate;

public class PredecessorUpdateObserver extends ChordObserver {

    public PredecessorUpdateObserver() {
        super(new ChordMessageKey("PREDECESSOR_UPDATE"));
    }

    @Override
    public boolean notify(ChordMessage message) {
        if (!(message instanceof PredecessorUpdate))
            return false;
        Node.get().handlePredecessorUpdate((PredecessorUpdate) message);
        return false;
    }

}
