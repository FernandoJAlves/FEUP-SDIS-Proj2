package dbs.chord;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Chord {

    // Chord protocol config: These must be the same for all nodes (should go without saying)
    public static final int m = 32;
    public static final BigInteger modulus = BigInteger.ONE.shiftLeft(m);

    // Reasonable small numbers here...
    public static final int MAX_JOIN_ATTEMPTS = 5;
    public static final int NODE_TASKS_POOL_SIZE = 2;
    public static final int DISPATCHER_TASKS_POOL_SIZE = 4;

    // All durations / delays / periods below are in milliseconds.
    public static final int STABILIZE_PERIOD = 1000;
    public static final int FIXFINGERS_PERIOD = 15000 / m; // full loop every 15s
    public static final int CHECK_PREDECESSOR_PERIOD = 2000;
    public static final int NODE_DUMP_PERIOD = FIXFINGERS_PERIOD * m + 500;

    // There is no strong guarantee that the delays will enforce the order in which the subprotocols are started.
    public static final int STABILIZE_DELAY = 0;
    public static final int FIXFINGERS_DELAY = 50;
    public static final int CHECK_PREDECESSOR_DELAY = 100;
    public static final int NODE_DUMP_DELAY = 200;

    // How long the TimeoutObservers wait before running timeout().
    public static final int CHECK_PREDECESSOR_WAIT = 1200; // AliveObserver
    public static final int LOOKUP_WAIT = 2000; // ResponsibleObserver
    public static final int JOIN_WAIT = 3000; // JoinObserver

    // How long after a failed join should the peer try again.
    public static final int MIN_JOIN_WAIT = 0;
    public static final int MAX_JOIN_WAIT = 1500;

    // Printing config
    private static final int percentPrecision = 2;

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

            return normalize(new BigInteger(md.digest(bytes)));
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    public static BigInteger consistentHash(InetSocketAddress socketAddress) {
        return consistentHash(socketAddress.getAddress(), socketAddress.getPort());
    }

    public static BigInteger encodeSHA256(String toEncode) {
        byte[] encodedString = {};
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            encodedString = digest.digest(toEncode.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
        return normalize(new BigInteger(encodedString));
    }

    // Function from: https://www.baeldung.com/sha-256-hashing-java
    public static String bytesToHex(byte[] bytes) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
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
     * @param i Any BigInteger, possibly out of bounds.
     * @return The chord id designated by this BigInteger.
     */
    public static BigInteger normalize(BigInteger i) {
        return relative(BigInteger.ZERO, i);
    }

    /**
     * Compare relative order of chord ids a, b, c.
     *
     * @param a,b,c chord ids.
     * @return -1 if a --> b --> c --> a; 0 if b == c; 1 if a --> c --> b --> a.
     */
    public static int compare(BigInteger a, BigInteger b, BigInteger c) {
        BigInteger ab = relative(a, b), ac = relative(a, c);
        return ab.compareTo(ac);
    }

    /**
     * @param a,b,c chord ids.
     * @return true if a --> b --> c in a non-strict way, i.e. they need not be
     *         distinct.
     */
    public static boolean ordered(BigInteger a, BigInteger b, BigInteger c) {
        return compare(a, b, c) <= 0;
    }

    /**
     * @param a,b,c chord ids.
     * @return true if a --> b --> c and a != b, a != c, but possibly b == c.
     */
    public static boolean afterOrdered(BigInteger a, BigInteger b, BigInteger c) {
        BigInteger ab = relative(a, b), ac = relative(a, c);
        return ab.signum() > 0 && ac.signum() > 0 && ab.compareTo(ac) <= 0;
    }

    /**
     * @param a,b,c chord ids.
     * @return true if a --> b --> c in a strict way, i.e. all three are distinct.
     */
    public static boolean strictOrdered(BigInteger a, BigInteger b, BigInteger c) {
        BigInteger ab = relative(a, b), ac = relative(a, c);
        return ab.signum() > 0 && ac.signum() > 0 && ab.compareTo(ac) < 0;
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
        assert i > 0;
        BigInteger finger = nodeId.add(BigInteger.ONE.shiftLeft(i - 1)).mod(modulus);
        while (finger.signum() < 0)
            finger = finger.add(modulus);
        return finger;
    }

    /**
     * Compute the relative position of the node on the chord as a percentage
     * distance from the origin id 0.
     *
     * @param id A chord id.
     * @return chordId / 2^m as double in range [0, 1).
     */
    public static double percent(BigInteger id) {
        return id.doubleValue() / modulus.doubleValue();
    }

    /**
     * Compute the relative distance of two nodes as a percentage distance of the
     * length of the entire chord.
     *
     * @param a,b chord ids.
     * @return (b - a) / 2^m as double in range [0, 1).
     */
    public static double percent(BigInteger a, BigInteger b) {
        return percent(relative(a, b));
    }

    private static final String percentFormat = "%0" + (3 + percentPrecision) + "." + percentPrecision + "f%%";

    /**
     * @param id a chord id.
     * @return percent(chordId) as a String %.
     */
    public static String percentStr(BigInteger id) {
        return String.format(percentFormat, percent(id) * 100.0);
    }

    /**
     * @param a,b chord ids.
     * @return percent(a,b) as a String %.
     */
    public static String percentStr(BigInteger a, BigInteger b) {
        return String.format(percentFormat, percent(a, b) * 100.0);
    }

    /**
     * @return Verbose print of the given node in the finger table.
     */
    public static String print(NodeInfo node) {
        return node == null ? "?" : node.toString();
    }

    /**
     * Given a chord id 'ID', and a replication degree 'R', compute the following
     * chord ids -- there are R of them, all evenly spaced around the Chord circle.
     *
     *                             | i * 2^m |
     *                    baseId + | ------- |, i = 0, 1, ..., R - 1.
     *                             |_   R   _|
     *
     * Precondition: 2R <= 2^m.
     *
     * @param baseId The base chord id.
     * @param i Compute the ith offset chord id.
     * @param R Replication degree.
     * @return The chord id given by the formula above.
     */
    public static BigInteger offset(BigInteger baseId, int i, int R) {
        assert i >= 0 && R > 0 && i < R;

        BigInteger bigI = BigInteger.valueOf(i);
        BigInteger bigR = BigInteger.valueOf(R);

        if (modulus.compareTo(bigR) >= 0)
            throw new IllegalArgumentException("Replication degree too large");

        return normalize(baseId.add(modulus.multiply(bigI).divide(bigR)));
    }

    /**
     * Like offset(baseId, i, R), but returns an array for each i.
     *
     * @param baseId The base chord id.
     * @param R Replication degree.
     * @return The chord ids array given by the formula above, for each i in range.
     */
    public static BigInteger[] offsets(BigInteger baseId, int R) {
        assert R > 0;

        BigInteger bigR = BigInteger.valueOf(R);

        if (modulus.compareTo(bigR) >= 0)
            throw new IllegalArgumentException("Replication degree too large");

        BigInteger[] ids = new BigInteger[R];

        for (int i = 0; i < R; ++i) {
            BigInteger bigI = BigInteger.valueOf(i);
            ids[i] = normalize(baseId.add(modulus.multiply(bigI).divide(bigR)));
        }

        return ids;
    }
}
