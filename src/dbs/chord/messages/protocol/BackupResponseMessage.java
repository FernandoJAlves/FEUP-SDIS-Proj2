package dbs.chord.messages.protocol;

import java.math.BigInteger;

import dbs.chord.Chord;
import dbs.chord.messages.ChordIdMessage;

public final class BackupResponseMessage extends ChordIdMessage {

    private final int code;

    public BackupResponseMessage(BigInteger fileId, int code) {
        super("BACKUPRESPONSE", fileId);
        this.code = code;
    }

    public int getResultCode() {
        return code;
    }

    public BigInteger getFileId() {
        return getChordId();
    }

    @Override
    public String toString() {
        return "BACKUPRESPONSE(" + Chord.percentStr(getChordId()) + ")";
    }
}
