package dbs.chord;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * The basic identifier of a Chord node: contains a chord id (BigInteger) for a
 * node and the node's server socket address (ip + port). This data structure is
 * fit for network communications, and serves to inform other Chord nodes where
 * to connect to this node.
 *
 * If all nodes use the same consistent hash provided in Chord, then
 *
 * chordId = Chord.consistentHash(serverAddress)
 */
public final class NodeServerInfo implements Serializable {

    public final BigInteger chordId;
    public final InetSocketAddress serverAddress;

    public NodeServerInfo(BigInteger chordId, InetSocketAddress serverAddress) {
        this.chordId = chordId;
        this.serverAddress = serverAddress;
    }

    public BigInteger getChordId() {
        return chordId;
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

    @Override
    public int hashCode() {
        return Objects.hash(chordId, serverAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof NodeServerInfo))
            return false;
        NodeServerInfo other = (NodeServerInfo) obj;
        return Objects.equals(chordId, other.chordId) && Objects.equals(serverAddress, other.serverAddress);
    }

    @Override
    public String toString() {
        return "node(" + chordId + ", " + serverAddress + ")";
    }
}
