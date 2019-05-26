package dbs.chord.observers;

import static dbs.chord.Chord.CHECK_PREDECESSOR_WAIT;

import dbs.chord.Node;
import dbs.chord.messages.AliveMessage;
import dbs.chord.messages.ChordMessage;

/**
 * Permanent observer.
 *
 * Bread-and-butter observer that receives is alive responses (liveness verifications).
 */
public final class AliveObserver extends TimeoutObserver {

    public AliveObserver() {
        super("ISALIVE", CHECK_PREDECESSOR_WAIT);
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof AliveMessage;
        Node.get().handleIsAlive((AliveMessage) message);
    }

    @Override
    public void timeout() {
        System.out.println("Did not receive response from predecessor node");
        Node.get().handleIsAliveTimeout();
    }

    @Override
    public String toString() {
        return "AliveObserver";
    }
}
