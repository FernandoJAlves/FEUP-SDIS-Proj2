package dbs.chord.observers.protocols;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.protocol.BackupResponseMessage;
import dbs.chord.observers.TimeoutObserver;

public final class BackupResponseObserver extends TimeoutObserver {

    public static final int BACKUPRESPONSE_WAIT = 2000;

    private final CompletableFuture<Integer> future;

    public BackupResponseObserver(BigInteger fileId, CompletableFuture<Integer> future) {
        super(new ChordIdKey("BACKUPRESPONSE", fileId), BACKUPRESPONSE_WAIT);
        this.future = future;
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof BackupResponseMessage;
        BackupResponseMessage response = (BackupResponseMessage) message;
        future.complete(response.getResultCode());
    }

    @Override
    public void timeout() {
        future.complete(null);
    }

    @Override
    public String toString() {
        return "BackupResponseObserver " + ((ChordIdKey) getSubscribedKey()).getChordId();
    }
}
