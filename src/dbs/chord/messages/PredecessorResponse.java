package dbs.chord.messages;

import dbs.chord.NodeInfo;

public final class PredecessorResponse extends ChordMessage {

    private final NodeInfo sourceNode;

    public PredecessorResponse(NodeInfo source) {
        super("PREDECESSOR_RESPONSE", null);
        this.sourceNode = source;
    }

    public NodeInfo getSourceNode() {
        return sourceNode;
    }
}
