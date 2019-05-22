package dbs.chord.messages;

import dbs.chord.NodeServerInfo;

public final class PredecessorResponse extends ChordMessage {

    private final NodeServerInfo sourceNode;

    public PredecessorResponse(NodeServerInfo source) {
        super("PREDECESSOR_RESPONSE");
        this.sourceNode = source;
    }

    public NodeServerInfo getSourceNode() {
        return sourceNode;
    }
}
