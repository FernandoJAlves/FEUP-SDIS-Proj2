package dbs.chord.messages;

import java.math.BigInteger;

/**
 * A responsible (response) is sent in response to a received Lookup (request)
 * message, to inform the source node that this node is the one responsible for
 * the queried key chordId.
 *
 * Responsible message may originate in Node.handleLookup(Lookup).
 */
public final class Responsible extends ChordMessage {

    public Responsible(BigInteger chordId) {
        super(new ChordIdKey("RESPONSIBLE", chordId));
    }
}
