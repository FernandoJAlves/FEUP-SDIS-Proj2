package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.KeepAliveMessage;

/**
 * Permanent observer.
 *
 * Bread-and-butter observer that receives keep alive requests (liveness checks).
 */
public final class KeepAliveObserver extends PermanentObserver {

    public KeepAliveObserver() {
        super("KEEPALIVE");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof KeepAliveMessage;
        Node.get().handleKeepAlive((KeepAliveMessage) message);
    }

    @Override
    public String toString() {
        return "KeepAliveObserver";
    }
}
