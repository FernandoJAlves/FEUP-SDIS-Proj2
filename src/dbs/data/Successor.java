package dbs.data;

import java.math.BigInteger;

import dbs.chord.NodeInfo;

public final class Successor extends ChordMessage {

    private static final long serialVersionUID = -4144915913628687114L;

    private final BigInteger chordid;
    private final NodeInfo originNode;

    public Successor(BigInteger chordid, NodeInfo originNode) {
        super("SUCCESSOR");
        this.chordid = chordid;
        this.originNode = originNode;
    }

    public BigInteger getId() {
        return chordid;
    }

    public NodeInfo getOriginNode() {
        return originNode;
    }
}
