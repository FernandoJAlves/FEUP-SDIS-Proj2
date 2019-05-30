package dbs.chord.observers.protocols;

import dbs.chord.Node;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.GetPredecessorMessage;
import dbs.chord.messages.protocol.BackupMessage;
import dbs.chord.observers.PermanentObserver;

public class BackupObserver extends PermanentObserver {

    public BackupObserver() {
        super("BACKUP");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof BackupMessage;
        //TODO: Funcao sandro
        Node.get().handleGetPredecessor((GetPredecessorMessage) message);
    }

    @Override
    public String toString() {
        return "BackupObserver";
    }
}
