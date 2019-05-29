package dbs.chord.observers;

import static dbs.chord.Chord.CHECK_PREDECESSOR_WAIT;

import dbs.chord.Node;
import dbs.chord.NodeInfo;
import dbs.chord.messages.AliveMessage;
import dbs.chord.messages.ChordMessage;

/**
 * Bread-and-butter observer that receives is alive responses (liveness verifications).
 */
public final class AliveObserver extends TimeoutObserver {

    private final NodeInfo waitedNode;

    public AliveObserver(NodeInfo waitedNode) {
        super("ISALIVE", CHECK_PREDECESSOR_WAIT);
        this.waitedNode = waitedNode;
    }

    public NodeInfo getPredecessorNode() {
        return waitedNode;
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof AliveMessage;
        Node.get().handleIsAlive((AliveMessage) message);
    }

    @Override
    public void timeout() {
        Node.get().handleIsAliveTimeout(waitedNode);
    }

    @Override
    public String toString() {
        return "AliveObserver";
    }
}
