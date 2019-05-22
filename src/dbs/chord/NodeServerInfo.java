package dbs.chord;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * The basic identifier of a Chord node: contains a chord id (BigInteger) for a
 * node and the node's server socket address (ip + port). This data structure is
 * fit for network communications, and serves to inform other Chord nodes where
 * to connect to this node.
 *
 * If all nodes use the same consistent hash provided in Chord, then
 *
 * chordid = Chord.consistentHash(serverAddress)
 */
public final class NodeServerInfo implements Serializable {

    public final BigInteger chordid;
    public final InetSocketAddress serverAddress;

    public NodeServerInfo(BigInteger chordid, InetSocketAddress serverAddress) {
        this.chordid = chordid;
        this.serverAddress = serverAddress;
    }

    public BigInteger getChordId() {
        return chordid;
    }

    public InetAddress getIp() {
        return serverAddress.getAddress();
    }

    public int getPort() {
        return serverAddress.getPort();
    }

    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }
}
