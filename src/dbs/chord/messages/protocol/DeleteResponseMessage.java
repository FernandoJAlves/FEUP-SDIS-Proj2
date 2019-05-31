package dbs.chord.messages.protocol;

import java.math.BigInteger;
import dbs.chord.Chord;
import dbs.chord.messages.ChordIdMessage;
import dbs.filesystem.threads.ResultCode;

public final class DeleteResponseMessage extends ChordIdMessage {

    private final ResultCode code;

    public DeleteResponseMessage(BigInteger fileId, ResultCode code) {
        super("DELETERESPONSE", fileId);
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
        return "DELETERESPONSE(" + Chord.percentStr(getChordId()) + ") [" + code + "]";
    }
}
