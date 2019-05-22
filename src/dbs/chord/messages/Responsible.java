package dbs.chord.messages;

import java.math.BigInteger;

import dbs.chord.NodeServerInfo;

/**
 * A responsible (response) is sent in response to a received Lookup (request)
 * message, to inform the source node that this node is the one responsible for
 * the queried key chordid.
 *
 * Responsible message may originate in Node.handleLookup(Lookup).
 */
public final class Responsible extends ChordMessage {

    private final NodeServerInfo sourceNode;
    private final NodeServerInfo responsibleNode;

    public Responsible(BigInteger chordid, NodeServerInfo source, NodeServerInfo responsible) {
        super(new LookupKey(chordid));
        this.sourceNode = source;
        this.responsibleNode = responsible;
    }

    public NodeServerInfo getSourceNode() {
        return sourceNode;
    }

    public NodeServerInfo getResponsible() {
        return responsibleNode;
    }
}
