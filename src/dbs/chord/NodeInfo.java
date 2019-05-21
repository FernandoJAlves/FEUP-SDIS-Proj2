package dbs.chord;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * The basic identifier of a Chord node: contains a chord id (BigInteger) and
 * the node's server socket address (ip + port).
 *
 * If all nodes use the same consistent hash provided in Chord, then
 *
 * chordid = Chord.consistentHash(socketAddress)
 */
public final class NodeInfo implements Serializable {

    private static final long serialVersionUID = 100100100000L;

    public final BigInteger chordid;
    public final InetSocketAddress socketAddress;

    public NodeInfo(BigInteger chordid, InetSocketAddress socketAddress) {
        this.chordid = chordid;
        this.socketAddress = socketAddress;
    }

    public NodeInfo(BigInteger chordid, InetAddress ip, int port) {
        this.chordid = chordid;
        this.socketAddress = new InetSocketAddress(ip, port);
    }

    public BigInteger getId() {
        return chordid;
    }

    public InetAddress getIp() {
        return socketAddress.getAddress();
    }

    public int getPort() {
        return socketAddress.getPort();
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }
}
