package dbs.chord.messages.protocol;

import dbs.chord.messages.ChordMessage;

public final class BackupMessage extends ChordMessage {

    public BackupMessage() {
        super("BACKUP");
    }

    @Override
    public String toString() {
        return "BACKUP";
    }
}
