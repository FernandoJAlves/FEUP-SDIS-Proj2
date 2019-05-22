package dbs.chord;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Chord {

    public static final int m = 16;
    public static final BigInteger modulus = BigInteger.ONE.shiftLeft(m);

    /**
     * Consistent hash, ugly implementation using a cryptographic hash function on
     * the result of concatenating the ip and port byte arrays.
     *
     * @param ip   The network address of the node.
     * @param port The network port of the node.
     * @return The Chord Id of the node.
     */
    public static BigInteger consistentHash(InetAddress ip, int port) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            byte[] ipBytes = ip.getAddress();
            byte[] portBytes = ("" + port).getBytes();
            byte[] bytes = new byte[ipBytes.length + portBytes.length];

            System.arraycopy(ipBytes, 0, bytes, 0, ipBytes.length);
            System.arraycopy(portBytes, 0, bytes, ipBytes.length, portBytes.length);

            return relative(BigInteger.ZERO, new BigInteger(md.digest(bytes)));
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    public static BigInteger consistentHash(InetSocketAddress socketAddress) {
        return consistentHash(socketAddress.getAddress(), socketAddress.getPort());
    }

    /**
     * Compare chord ids, modulo 2^m.
     *
     * @param pre A chord id.
     * @param suc A chord id.
     * @return suc - pre modulo 2^m, so that it is always positive. This means that:
     *         relative(a, b) < relative(a, c) implies a -> b -> c.
     */
    public static BigInteger relative(BigInteger pre, BigInteger suc) {
        BigInteger rel = suc.subtract(pre).mod(modulus);
        while (rel.signum() < 0)
            rel = rel.add(modulus);
        return rel;
    }

    /**
     * Compare relative order of chord ids a, b, c.
     *
     * @return -1 if a --> b --> c, 0 if b == c, and 1 if a --> c --> b.
     */
    public static int compare(BigInteger a, BigInteger b, BigInteger c) {
        BigInteger ab = relative(a, b), ac = relative(a, c);
        return ab.compareTo(ac);
    }
}
