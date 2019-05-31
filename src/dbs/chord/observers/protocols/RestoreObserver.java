package dbs.chord.observers.protocols;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import dbs.chord.NodeInfo;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.protocol.DeleteMessage;
import dbs.chord.messages.protocol.RestoreMessage;
import dbs.chord.messages.protocol.RestoreResponseMessage;
import dbs.chord.observers.PermanentObserver;
import dbs.filesystem.FileManager;
import dbs.filesystem.threads.ResultCode;
import dbs.network.SocketManager;

public class RestoreObserver extends PermanentObserver {

    public RestoreObserver() {
        super("RESTORE");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof RestoreMessage;
        
        NodeInfo remoteNode = message.getSender();
        RestoreMessage response = (RestoreMessage) message;
        CompletableFuture<byte[]> future = FileManager.getInstance().launchRestoreReader(response.getFileId());

        byte[] file;
        try {
            file = future.get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }

        RestoreResponseMessage responseMessage = new RestoreResponseMessage(response.getFileId(), file, ResultCode.OK);
        SocketManager.get().sendMessage(remoteNode, responseMessage);
    }

    @Override
    public String toString() {
        return "RestoreObserver";
    }
}
