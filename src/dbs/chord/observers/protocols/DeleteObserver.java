package dbs.chord.observers.protocols;

import java.util.concurrent.CompletableFuture;

import dbs.chord.NodeInfo;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.protocol.DeleteMessage;
import dbs.chord.messages.protocol.DeleteResponseMessage;
import dbs.chord.observers.PermanentObserver;
import dbs.filesystem.FileManager;
import dbs.filesystem.threads.ResultCode;
import dbs.network.SocketManager;

public class DeleteObserver extends PermanentObserver {

    public DeleteObserver() {
        super("DELETE");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof DeleteMessage;
        
        NodeInfo remoteNode = message.getSender();
        DeleteMessage response = (DeleteMessage) message;
        FileManager.getInstance().launchEraser(response.getFileId());

        DeleteResponseMessage responseMessage = new DeleteResponseMessage(response.getFileId(), ResultCode.OK);
        SocketManager.get().sendMessage(remoteNode, responseMessage);
    }

    @Override
    public String toString() {
        return "DeleteObserver";
    }
}
