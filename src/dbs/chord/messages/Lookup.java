package dbs.chord.messages;

import java.math.BigInteger;

import dbs.chord.NodeServerInfo;

/**
 * A lookup (request) is sent from a given source node N to find the chord Node
 * responsible for a given key chordId. A LookupObserver is set to await a
 * response of type Responsible for the same chordId.
 *
 * Lookup may originate in Node.lookup(chordId) or be received and forwarded in
 * Node.handleLookup(Lookup), as our implementation of Chord is recursive rather
 * than iterative. This class is not called 'LookupRequest' because the response
 * to this message is not (necessarily) sent by the node that received it, again
 * because our implementation is recursive and not iterative.
 */
public final class Lookup extends ChordMessage {

    private final NodeServerInfo sourceNode;

    public Lookup(BigInteger chordId, NodeServerInfo source) {
        super(new ChordIdKey("LOOKUP", chordId));
        this.sourceNode = source;
    }

    public NodeServerInfo getSourceNode() {
        return sourceNode;
    }

    public BigInteger getChordId() {
        return ((ChordIdKey) getKey()).getChordId();
    }
}
