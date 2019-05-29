package dbs.chord.observers;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.GetPredecessorMessage;

public class GetPredecessorObserver extends PermanentObserver {

    public GetPredecessorObserver() {
        super("GETPREDECESSOR");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof GetPredecessorMessage;
        Node.get().handleGetPredecessor((GetPredecessorMessage) message);
    }

    @Override
    public String toString() {
        return "GetPredecessorObserver";
    }
}
