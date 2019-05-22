package dbs.chord.observers;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import dbs.chord.NodeLocalInfo;
import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.Responsible;

public final class ResponsibleObserver extends ChordObserver {

    private final CompletableFuture<NodeLocalInfo> future;

    public ResponsibleObserver(BigInteger chordId, CompletableFuture<NodeLocalInfo> future) {
        super(new ChordIdKey("RESPONSIBLE", chordId));
        this.future = future;
    }

    @Override
    public boolean notify(ChordMessage message, NodeLocalInfo sender) {
        if (!(message instanceof Responsible))
            return false;
        this.future.complete(sender);
        return true;
    }
}
