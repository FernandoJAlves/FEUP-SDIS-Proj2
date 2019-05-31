package dbs.chord.observers.protocols;

import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.protocol.DeleteMessage;
import dbs.chord.observers.PermanentObserver;

public class DeleteObserver extends PermanentObserver {

    public DeleteObserver() {
        super("DELETE");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof DeleteMessage;
        
        //TODO: Funcao delete!
        // SomeClass.get().handleBackup((BackupMessage) message);
    }

    @Override
    public String toString() {
        return "DeleteObserver";
    }
}
