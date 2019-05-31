package dbs.chord.observers.protocols;

import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.protocol.BackupMessage;
import dbs.chord.messages.protocol.BackupResponseMessage;
import dbs.chord.observers.PermanentObserver;

public class BackupObserver extends PermanentObserver {

    public BackupObserver() {
        super("BACKUP");
    }

    @Override
    public void notify(ChordMessage message) {
        assert message instanceof BackupMessage;
        String s = new String(((BackupMessage) message).getFileContent());
        System.out.println("DA BACKUP: " + s);
        //TODO: Funcao sandro
        // SomeClass.get().handleBackup((BackupMessage) message);
    }

    @Override
    public String toString() {
        return "BackupObserver";
    }
}
