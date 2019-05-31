package dbs.chord.messages.protocol;

import java.math.BigInteger;

import dbs.chord.Chord;
import dbs.chord.messages.ChordMessage;

public final class DeleteMessage extends ChordMessage {

    private final BigInteger fileId;

    public DeleteMessage(BigInteger fileId) {
        super("DELETE");
        this.fileId = fileId;
    }

    public BigInteger getFileId() {
        return fileId;
    }

    @Override
    public String toString() {
        return "BACKUP(file " + Chord.percentStr(fileId) + ")";
    }
}
