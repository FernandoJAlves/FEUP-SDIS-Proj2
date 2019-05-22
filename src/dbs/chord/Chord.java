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
     * Retrieve the relative position of b relative to a, modulo 2^m.
     *
     * In other words, this returns what would be the chord id of b if chord id a
     * were 0, i.e. the origin of the Chord.
     *
     * @param a A chord id.
     * @param b A chord id.
     * @return b - a modulo 2^m, so that it is always nonnegative. This means that:
     *         relative(a, b) < relative(a, c) implies a -> b -> c --> a.
     */
    public static BigInteger relative(BigInteger a, BigInteger b) {
        BigInteger rel = b.subtract(a).mod(modulus);
        while (rel.signum() < 0)
            rel = rel.add(modulus);
        return rel;
    }

    /**
     * Compare relative order of chord ids a, b, c.
     *
     * @param a,b,c Chord ids.
     * @return -1 if a --> b --> c --> a; 0 if b == c; 1 if a --> c --> b --> a.
     */
    public static int compare(BigInteger a, BigInteger b, BigInteger c) {
        BigInteger ab = relative(a, b), ac = relative(a, c);
        return ab.compareTo(ac);
    }

    /**
     * Compute the smallest possible key that is a valid ith finger of the node with
     * the given chord id.
     *
     * @param nodeId The chord id of a node.
     * @param i      The finger index (starts at 1, ends at m).
     * @return The chord id of the finger.
     */
    public static BigInteger ithFinger(BigInteger nodeId, int i) {
        BigInteger finger = nodeId.add(BigInteger.TWO.shiftLeft(i - 1)).mod(modulus);
        while (finger.signum() < 0)
            finger = finger.add(modulus);
        return finger;
    }
}
