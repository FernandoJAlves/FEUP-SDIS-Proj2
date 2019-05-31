package dbs.chord.observers.protocols;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.protocol.BackupResponseMessage;
import dbs.chord.messages.protocol.DeleteResponseMessage;
import dbs.chord.observers.TimeoutObserver;
import dbs.filesystem.threads.ResultCode;

public final class DeleteResponseObserver extends TimeoutObserver {

    public static final int DELETERESPONSE_WAIT = 2000;

    private final CompletableFuture<ResultCode> future;
    
    public DeleteResponseObserver(BigInteger fileId, CompletableFuture<ResultCode> future) {
        super(new ChordIdKey("DELETERESPONSE", fileId), DELETERESPONSE_WAIT);
        this.future = future;
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof DeleteResponseMessage;
        DeleteResponseMessage response = (DeleteResponseMessage) message;
        future.complete(response.getResultCode());
    }

    @Override
    public void timeout() {
        future.complete(null);
    }

    @Override
    public String toString() {
        return "DeleteResponseObserver " + ((ChordIdKey) getSubscribedKey()).getChordId();
    }
}
