package dbs.chord;

import java.math.BigInteger;
import java.net.InetSocketAddress;

/**
 * The basic identifier of a Chord node for internal use: contains a chord id
 * (BigInteger) for a node and its corresponding local socket address (key for
 * the SocketManager). This data structure is for internal use only.
 */
public final class NodeLocalInfo {

    public final BigInteger chordid;
    public final InetSocketAddress localAddress;

    public NodeLocalInfo(BigInteger chordid, InetSocketAddress localAddress) {
        this.chordid = chordid;
        this.localAddress = localAddress;
    }

    public BigInteger getChordId() {
        return chordid;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }
}
