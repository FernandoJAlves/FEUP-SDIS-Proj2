package dbs.chord;

import java.math.BigInteger;
import java.net.InetSocketAddress;

/**
 * The basic identifier of a Chord node for internal use: contains a chord id
 * (BigInteger) for a node and its corresponding local socket address (key for
 * the SocketManager). This data structure is for internal use only.
 */
public final class NodeLocalInfo {

    public final BigInteger chordId;
    public final InetSocketAddress localAddress;
    public final InetSocketAddress serverAddress;

    public NodeLocalInfo(BigInteger chordId, InetSocketAddress localAddress, InetSocketAddress serverAddress) {
        this.chordId = chordId;
        this.localAddress = localAddress;
        this.serverAddress = serverAddress;
    }

    public NodeLocalInfo(NodeServerInfo serverInfo, InetSocketAddress localAddress) {
        this.chordId = serverInfo.getChordId();
        this.localAddress = localAddress;
        this.serverAddress = serverInfo.getServerAddress();
    }

    public BigInteger getChordId() {
        return chordId;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }

    public NodeServerInfo getServerInfo() {
        return new NodeServerInfo(chordId, serverAddress);
    }
}
