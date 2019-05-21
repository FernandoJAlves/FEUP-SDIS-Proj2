package dbs.chord.messages;

import dbs.chord.NodeInfo;

public final class PredecessorUpdate extends ChordMessage {

    private final NodeInfo sourceNode;

    public PredecessorUpdate(NodeInfo source) {
        super("PREDECESSOR_UPDATE", null);
        this.sourceNode = source;
    }

    public NodeInfo getSourceNode() {
        return sourceNode;
    }
}
