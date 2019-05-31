package dbs.chord.messages;

import java.math.BigInteger;
import dbs.chord.Chord;
import dbs.chord.NodeInfo;

/**
 * A lookup (request) is sent from a given source node N to find the chord Node
 * responsible for a given key chordId. A LookupObserver is set to await a
 * response of type Responsible for the same chordId.
 *
 * LookupMessage may originate in Node.lookup(chordId) or be received and
 * forwarded in Node.handleLookup(LookupMessage), as our implementation of Chord
 * is recursive rather than iterative. This class is not called 'LookupRequest'
 * because the response to this message is not (necessarily) sent by the node
 * that received it, again because our implementation is recursive and not
 * iterative.
 */
public final class LookupMessage extends ForwardingMessage {

    private final NodeInfo sourceNode;
    private final BigInteger chordId;

    public LookupMessage(BigInteger chordId, NodeInfo source) {
        super("LOOKUP");
        this.sourceNode = source;
        this.chordId = chordId;
    }

    public LookupMessage(LookupMessage message) {
        super(message);
        this.sourceNode = message.sourceNode;
        this.chordId = message.chordId;
    }

    public NodeInfo getSourceNode() {
        return sourceNode;
    }

    public BigInteger getChordId() {
        return chordId;
    }

    @Override
    public String toString() {
        return "LOOKUP(" + Chord.percentStr(chordId) + ", source " + sourceNode.shortStr() + ")";
    }
}
