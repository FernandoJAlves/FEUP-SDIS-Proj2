package dbs.chord.observers;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import dbs.chord.NodeServerInfo;
import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.Responsible;

public final class ResponsibleObserver extends ChordObserver {

    private final CompletableFuture<NodeServerInfo> future;

    public ResponsibleObserver(BigInteger chordId, CompletableFuture<NodeServerInfo> future) {
        super(new ChordIdKey("RESPONSIBLE", chordId));
        this.future = future;
    }

    @Override
    public boolean notify(ChordMessage message) {
        if (!(message instanceof Responsible))
            return false;
        this.future.complete(message.getSender());
        return true;
    }
}
