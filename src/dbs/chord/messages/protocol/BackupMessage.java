package dbs.chord.messages.protocol;

import java.math.BigInteger;

import dbs.chord.Chord;
import dbs.chord.messages.ChordMessage;

public final class BackupMessage extends ChordMessage {

    private final BigInteger fileId;
    private final byte[] file;
    private final boolean overwrite;

    public BackupMessage(BigInteger fileId, byte[] file, boolean overwrite) {
        super("BACKUP");
        this.fileId = fileId;
        this.file = file;
        this.overwrite = overwrite;
    }

    public BackupMessage(BigInteger fileId, byte[] file) {
        super("BACKUP");
        this.fileId = fileId;
        this.file = file;
        this.overwrite = false;
    }

    public BigInteger getFileId() {
        return fileId;
    }

    public byte[] getFileContent() {
        return file;
    }

    public boolean allowsOverwrite() {
        return overwrite;
    }

    @Override
    public String toString() {
        return "BACKUP(file " + Chord.percentStr(fileId) + ")";
    }
}
