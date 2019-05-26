package dbs.chord.messages;

import dbs.chord.NodeInfo;

public final class PredecessorMessage extends ChordMessage {

    private final NodeInfo predecessorNode;

    public PredecessorMessage(NodeInfo predecessor) {
        super("PREDECESSOR");
        this.predecessorNode = predecessor;
    }

    public NodeInfo getPredecessorNode() {
        return predecessorNode;
    }

    @Override
    public String toString() {
        return "PREDECESSOR [" + predecessorNode.shortStr() + "]";
    }
}
