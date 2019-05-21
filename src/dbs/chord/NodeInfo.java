package dbs.chord;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;

public final class NodeInfo implements Serializable {

    private static final long serialVersionUID = 100100100000L;

    public final BigInteger chordid;
    public final InetAddress ip;
    public final int port;

    public NodeInfo(BigInteger chordid, InetAddress ip, int port) {
        this.chordid = chordid;
        this.ip = ip;
        this.port = port;
    }

    public BigInteger getId() {
        return chordid;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
