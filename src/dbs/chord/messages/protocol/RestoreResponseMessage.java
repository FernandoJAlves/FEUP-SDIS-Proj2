package dbs.chord.messages.protocol;

import java.math.BigInteger;
import dbs.chord.Chord;
import dbs.chord.messages.ChordIdMessage;
import dbs.filesystem.threads.ResultCode;

public final class RestoreResponseMessage extends ChordIdMessage {

    private final byte[] file;
    private final ResultCode code;


    public RestoreResponseMessage(BigInteger fileId, byte[] file, ResultCode code) {
        super("RESTORERESPONSE", fileId);
        this.file = file;
        this.code = code;
    }

    public ResultCode getResultCode() {
        return code;
    }

    public BigInteger getFileId() {
        return getChordId();
    }

    public byte[] getFileContent() {
        return file;
    }

    @Override
    public String toString() {
        return "RESTORERESPONSE(" + Chord.percentStr(getChordId()) + ") [" + code + "]";
    }
}
