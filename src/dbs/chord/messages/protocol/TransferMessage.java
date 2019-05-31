package dbs.chord.messages.protocol;

import java.math.BigInteger;
import dbs.chord.Chord;
import dbs.chord.messages.ChordMessage;

public final class TransferMessage extends ChordMessage {

    private final BigInteger fileId;
    private final byte[] file;

    public TransferMessage(BigInteger fileId, byte[] file) {
        super("TRANSFER");
        this.fileId = fileId;
        this.file = file;
    }

    public BigInteger getFileId() {
        return fileId;
    }

    public byte[] getFileContent() {
        return file;
    }

    @Override
    public String toString() {
        return "TRANSFER(file " + Chord.percentStr(fileId) + ")";
    }
}
