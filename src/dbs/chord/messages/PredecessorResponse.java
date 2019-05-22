package dbs.chord.messages;

import dbs.chord.NodeServerInfo;

public final class PredecessorResponse extends ChordMessage {

    private final NodeServerInfo predecessorNode;

    public PredecessorResponse(NodeServerInfo predecessor) {
        super(new ChordMessageKey("PREDECESSOR_RESPONSE"));
        this.predecessorNode = predecessor;
    }

    public NodeServerInfo getPredecessorNode() {
        return predecessorNode;
    }
}
