package dbs.chord.observers.protocols;

import java.io.IOException;

import dbs.chord.NodeInfo;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.protocol.BackupMessage;
import dbs.chord.messages.protocol.BackupResponseMessage;
import dbs.chord.observers.PermanentObserver;
import dbs.filesystem.Configuration;
import dbs.filesystem.FileManager;
import dbs.filesystem.threads.ResultCode;
import dbs.filesystem.threads.Writer;
import dbs.network.SocketManager;

public class BackupObserver extends PermanentObserver {

    public BackupObserver() {
        super("BACKUP");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof BackupMessage;
        BackupMessage backupMessage = (BackupMessage) message;

        NodeInfo remoteNode = message.getSender();
        
        String fileKey = new String(backupMessage.getFileId().toByteArray());
        Writer writer; 
       
        try {
            writer = new Writer(fileKey, backupMessage.getFileContent(), Configuration.Operation.BACKUP);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
    
        FileManager.getInstance().getThreadpool().submit(writer);

        //TODO: Handle writer response
        BackupResponseMessage responseMessage = new BackupResponseMessage(backupMessage.getFileId(), ResultCode.OK);
        SocketManager.get().sendMessage(remoteNode, responseMessage);
    }

    @Override
    public String toString() {
        return "BackupObserver";
    }
}
