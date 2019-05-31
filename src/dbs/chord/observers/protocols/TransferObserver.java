package dbs.chord.observers.protocols;

import java.math.BigInteger;

import dbs.chord.Chord;
import dbs.chord.ChordLogger;
import dbs.chord.Node;
import dbs.chord.NodeInfo;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.protocol.TransferMessage;
import dbs.chord.observers.PermanentObserver;
import dbs.filesystem.FileManager;

public class TransferObserver extends PermanentObserver {

    public TransferObserver() {
        super("TRANSFER");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof TransferMessage;
        TransferMessage transferMessage = (TransferMessage) message;

        BigInteger fileId = transferMessage.getFileId();
        byte[] file = transferMessage.getFileContent();

        NodeInfo predecessorNode = Node.get().getPredecessor();
        NodeInfo selfNode = Node.get().getSelf();

        if (Chord.afterOrdered(predecessorNode.getChordId(), fileId, selfNode.getChordId())) {
            FileManager.getInstance().launchBackupWriter(fileId, file);

            String shortFile = Chord.percentStr(fileId), shortSender = transferMessage.getSender().shortStr();
            ChordLogger.logTransfer("Received and stored " + shortFile + " from " + shortSender);
        }
    }
}
