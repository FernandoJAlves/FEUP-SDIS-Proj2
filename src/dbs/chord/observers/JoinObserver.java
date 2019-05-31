package dbs.chord.observers;

import static dbs.chord.Chord.JOIN_WAIT;
import dbs.chord.Node;
import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ResponsibleMessage;

/**
 * Used when the Node's join(remote) method asks the initiator (remote) node for
 * this new node's successor. The notify method then sets this node's proper
 * successor.
 */
public final class JoinObserver extends TimeoutObserver {

    public JoinObserver() {
        super(new ChordIdKey("RESPONSIBLE", Node.get().getSelf().getChordId()), JOIN_WAIT);
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof ResponsibleMessage;
        Node.get().handleJoinResponse((ResponsibleMessage) message);
    }

    @Override
    public void timeout() {
        Node.get().handleFailedJoin();
    }

    @Override
    public String toString() {
        return "JoinObserver " + ((ChordIdKey) getSubscribedKey()).getChordId();
    }
}
