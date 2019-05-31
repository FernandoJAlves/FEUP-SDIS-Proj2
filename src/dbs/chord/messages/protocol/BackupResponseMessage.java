package dbs.chord.messages.protocol;

import java.math.BigInteger;
import dbs.chord.Chord;
import dbs.chord.messages.ChordIdMessage;
import dbs.filesystem.threads.ResultCode;

public final class BackupResponseMessage extends ChordIdMessage {

    private final ResultCode code;

    public BackupResponseMessage(BigInteger fileId, ResultCode code) {
        super("BACKUPRESPONSE", fileId);
        this.code = code;
    }

    public ResultCode getResultCode() {
        return code;
    }

    public BigInteger getFileId() {
        return getChordId();
    }

    @Override
    public String toString() {
        return "BACKUPRESPONSE(" + Chord.percentStr(getChordId()) + ") [" + code + "]";
    }
}
