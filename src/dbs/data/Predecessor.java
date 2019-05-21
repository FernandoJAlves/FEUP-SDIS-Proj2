package dbs.data;

import java.math.BigInteger;

import dbs.chord.NodeInfo;

public final class Predecessor extends ChordMessage {

    private static final long serialVersionUID = 58373264321240382L;

    private final BigInteger chordid;
    private final NodeInfo originNode;

    public Predecessor(BigInteger chordid, NodeInfo originNode) {
        super("PREDECESSOR");
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
