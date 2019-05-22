package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.messages.PredecessorResponse;

public class PredecessorResponseObserver extends ChordObserver {

    protected PredecessorResponseObserver() {
        super(new ChordMessageKey("PREDECESSOR_RESPONSE"));
    }

    @Override
    public boolean notify(ChordMessage message) {
        if (!(message instanceof PredecessorResponse))
            return false;
        Node.get().handlePredecessorResponse((PredecessorResponse) message);
        return false;
    }

}
