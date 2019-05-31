package dbs.chord.observers;

import static dbs.chord.Chord.LOOKUP_WAIT;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import dbs.chord.NodeInfo;
import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ResponsibleMessage;

public final class ResponsibleObserver extends TimeoutObserver {

    private final CompletableFuture<NodeInfo> future;

    public ResponsibleObserver(BigInteger chordId, CompletableFuture<NodeInfo> future) {
        super(new ChordIdKey("RESPONSIBLE", chordId), LOOKUP_WAIT);
        this.future = future;
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof ResponsibleMessage;
        future.complete(message.getSender());
    }

    @Override
    public void timeout() {
        future.complete(null);
    }

    @Override
    public String toString() {
        return "ResponsibleObserver " + ((ChordIdKey) getSubscribedKey()).getChordId();
    }
}
