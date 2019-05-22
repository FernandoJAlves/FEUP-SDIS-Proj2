package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.NodeLocalInfo;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.messages.PredecessorUpdate;

public class PredecessorUpdateObserver extends ChordObserver {

    protected PredecessorUpdateObserver() {
        super(new ChordMessageKey("PREDECESSOR_UPDATE"));
    }

    @Override
    public boolean notify(ChordMessage message, NodeLocalInfo sender) {
        if (!(message instanceof PredecessorUpdate))
            return false;
        Node.get().predecessorUpdate(sender);
        return false;
    }

}
