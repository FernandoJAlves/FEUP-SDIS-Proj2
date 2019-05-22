package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.Responsible;

/**
 * One-time observer
 *
 * Used when the Node's join(remote) method asks the initiator (remote) node for
 * this new node's successor. The notify method then sets this node's proper
 * successor.
 */
public final class JoinObserver extends ChordObserver {

    public JoinObserver() {
        super(new ChordIdKey("RESPONSIBLE", Node.get().getSelf().getChordId()));
    }

    @Override
    public boolean notify(ChordMessage message) {
        if (!(message instanceof Responsible))
            return false;
        Node.get().resolveSuccessor((Responsible) message);
        return true;
    }
}
