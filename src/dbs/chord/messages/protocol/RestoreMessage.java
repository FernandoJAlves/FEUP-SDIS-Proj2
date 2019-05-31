package dbs.chord.messages.protocol;

import java.math.BigInteger;

import dbs.chord.Chord;
import dbs.chord.messages.ChordMessage;

public final class RestoreMessage extends ChordMessage {

    private final BigInteger fileId;

    public RestoreMessage(BigInteger fileId) {
        super("RESTORE");
        this.fileId = fileId;
    }

    public BigInteger getFileId() {
        return fileId;
    }

    @Override
    public String toString() {
        return "RESTORE(file " + Chord.percentStr(fileId) + ")";
    }
}
