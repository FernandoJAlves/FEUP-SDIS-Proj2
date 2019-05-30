package dbs.chord.messages.protocol;

import dbs.chord.messages.ChordMessage;

public final class BackupResponseMessage extends ChordMessage {

    public BackupResponseMessage() {
        super("BACKUPRESPONSE");
    }

    @Override
    public String toString() {
        return "BACKUPRESPONSE";
    }
}
