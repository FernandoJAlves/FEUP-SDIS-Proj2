package dbs.chord.observers.protocols;

import dbs.chord.messages.ChordMessage;
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
        // SomeClass.get().handleBackup((BackupMessage) message);
    }

    @Override
    public String toString() {
        return "BackupObserver";
    }
}
