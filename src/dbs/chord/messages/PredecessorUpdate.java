package dbs.chord.messages;

import dbs.chord.NodeServerInfo;

public final class PredecessorUpdate extends ChordMessage {

    private final NodeServerInfo sourceNode;

    public PredecessorUpdate(NodeServerInfo source) {
        super("PREDECESSOR_UPDATE");
        this.sourceNode = source;
    }

    public NodeServerInfo getSourceNode() {
        return sourceNode;
    }
}
