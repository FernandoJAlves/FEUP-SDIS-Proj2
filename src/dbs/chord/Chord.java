package dbs.chord;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Chord {

    private final int m;
    private final BigInteger modulus;

    private static Chord instance;

    public static Chord get() {
        return instance;
    }

    Chord(int m) {
        this.m = m;
        this.modulus = BigInteger.ONE.shiftLeft(m);
    }

    /**
     * Consistent hash, ugly implementation using a cryptographic hash function on
     * the result of concatenating the ip and port byte arrays.
     *
     * @param ip   The network address of the node.
     * @param port The network port of the node.
     * @return The Chord Id of the node.
     */
    public BigInteger nodeConsistentHash(InetAddress ip, int port) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            byte[] ipBytes = ip.getAddress();
            byte[] portBytes = ("" + port).getBytes();
            byte[] bytes = new byte[ipBytes.length + portBytes.length];

            System.arraycopy(ipBytes, 0, bytes, 0, ipBytes.length);
            System.arraycopy(portBytes, 0, bytes, ipBytes.length, portBytes.length);

            BigInteger hash = new BigInteger(md.digest(bytes)).mod(modulus);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Compare chord ids, modulo 2^m.
     *
     * @param pre A chord id.
     * @param suc A chord id.
     * @return suc - pre modulo 2^m, so that it is always positive. This means that:
     *         relative(a, b) < relative(a, c) implies a -> b -> c.
     */
    public BigInteger relative(BigInteger pre, BigInteger suc) {
        BigInteger rel = suc.subtract(pre).mod(modulus);
        if (rel.signum() < 0)
            rel = rel.add(modulus);
        return rel;
    }
}
