package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.NotifyMessage;

/**
 * Permanent observer.
 *
 * Bread-and-butter observer that receives notify messages (predecessor information).
 */
public final class NotifyObserver extends PermanentObserver {

    public NotifyObserver() {
        super("NOTIFY");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof NotifyMessage;
        Node.get().handleNotify((NotifyMessage) message);
    }

    @Override
    public String toString() {
        return "NotifyObserver";
    }
}
