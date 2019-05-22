package dbs.chord.messages;

import dbs.chord.NodeInfo;

public final class PredecessorResponse extends ChordMessage {

    private final NodeInfo predecessorNode;

    public PredecessorResponse(NodeInfo predecessor) {
        super(new ChordMessageKey("PREDECESSOR_RESPONSE"));
        this.predecessorNode = predecessor;
    }

    public NodeInfo getPredecessorNode() {
        return predecessorNode;
    }
}
