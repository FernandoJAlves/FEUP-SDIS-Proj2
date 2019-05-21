package dbs.chord.messages;

import java.math.BigInteger;

import dbs.chord.NodeInfo;

/**
 * A responsible (response) is sent in response to a received Lookup (request)
 * message, to inform the source node that this node is the one responsible for
 * the queried key chordid.
 *
 * Responsible message may originate in Node.handleLookup(Lookup).
 */
public final class Responsible extends ChordMessage {

    private final NodeInfo sourceNode;
    private final NodeInfo responsibleNode;

    public Responsible(BigInteger chordid, NodeInfo source, NodeInfo responsible) {
        super("RESPONSIBLE", chordid);
        this.sourceNode = source;
        this.responsibleNode = responsible;
    }

    public NodeInfo getSourceNode() {
        return sourceNode;
    }

    public NodeInfo getResponsible() {
        return responsibleNode;
    }
}
