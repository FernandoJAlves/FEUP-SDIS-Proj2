package dbs.chord.observers;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import dbs.chord.NodeLocalInfo;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.LookupKey;
import dbs.chord.messages.Responsible;

public final class LookupObserver extends ChordObserver {

    private final CompletableFuture<NodeLocalInfo> future;

    public LookupObserver(BigInteger chordid, CompletableFuture<NodeLocalInfo> future) {
        super(new LookupKey(chordid));
        this.future = future;
    }

    @Override
    public boolean notify(ChordMessage message, InetSocketAddress localAddress) {
        if (!(message instanceof Responsible))
            return false;
        Responsible responsible = (Responsible) message;
        BigInteger responsibleId = responsible.getResponsible().getChordId();
        this.future.complete(new NodeLocalInfo(responsibleId, localAddress));
        return true;
    }
}
