package dbs.chord.observers.protocols;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import dbs.chord.RestoreReturn;
import dbs.chord.messages.ChordIdKey;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.protocol.RestoreResponseMessage;
import dbs.chord.observers.TimeoutObserver;
import dbs.filesystem.FileManager;
import dbs.filesystem.threads.ResultCode;

public final class RestoreResponseObserver extends TimeoutObserver {

    public static final int RESTORERESPONSE_WAIT = 2000;

    private final CompletableFuture<RestoreReturn> future;
    
    public RestoreResponseObserver(BigInteger fileId, CompletableFuture<RestoreReturn> future) {
        super(new ChordIdKey("RESTORERESPONSE", fileId), RESTORERESPONSE_WAIT);
        this.future = future;
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof RestoreResponseMessage;
        RestoreResponseMessage response = (RestoreResponseMessage) message;
        RestoreReturn ret = new RestoreReturn(response.getResultCode(), response.getFileContent());
        FileManager.getInstance().launchRestoreWriter(response.getFileId().toString(), response.getFileContent());
        future.complete(ret);
    }

    @Override
    public void timeout() {
        future.complete(null);
    }

    @Override
    public String toString() {
        return "RestoreResponseObserver " + ((ChordIdKey) getSubscribedKey()).getChordId();
    }
}
