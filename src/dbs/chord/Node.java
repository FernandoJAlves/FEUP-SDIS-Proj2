package dbs.chord;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class Node {

    /**
     * Threadpool's number of threads
     */
    private final int POOL_SIZE = 5;

    /**
     * Threadpool initialization
     */
    private ExecutorService threadPool = Executors.newFixedThreadPool(POOL_SIZE);

    private final NodeInfo self;
    private final AtomicReference<NodeInfo> predecessor;
    private final AtomicReferenceArray<NodeInfo> finger;

    private static Node instance;

    public static Node get() {
        return instance;
    }

    public static Node create(InetSocketAddress socketAddress) {
        return new Node(socketAddress);
    }

    Node(InetSocketAddress socketAddress) {
        assert instance == null;
        this.self = new NodeInfo(Chord.consistentHash(socketAddress), socketAddress);
        this.predecessor = new AtomicReference<>();
        this.finger = new AtomicReferenceArray<>(Chord.m);
        instance = this;
    }

    /**
     * Primary lookup interface. Returns a promise that will eventually resolve to
     * the node responsible for the given chordid, i.e. its successor.
     */
    public CompletableFuture<NodeInfo> lookup(BigInteger chordid) {
        // If this node is responsible, return ourselves.
        if (isResponsible(chordid)) {
            return CompletableFuture.completedFuture(self);
        }

        return null;
    }

    /**
     *
     */
    private NodeInfo lookupClosestPreceding(BigInteger chordid) {
        BigInteger relChord = Chord.relative(self.getId(), chordid);

        // self --> ... next ... --> chordid --> ... --> ... --> self

        for (int i = Chord.m; i > 0; --i) {
            NodeInfo next = finger.get(i);
            if (next == null)
                continue;

            BigInteger nextid = next.getId();
            BigInteger relNext = Chord.relative(self.getId(), nextid);

            if (relNext.compareTo(relChord) <= 0)
                return next;
        }

        return self;
    }

    /**
     * @return true if this Node is responsible for the given chord id.
     */
    private boolean isResponsible(BigInteger chordid) {
        // Return true if predecessorid -> chordid ==> selfid, which is:
        // 0 < relative(predecessorid, chordid) <= relative(predecessorid, selfid)
        BigInteger predecessorid = predecessor.get().getId();
        BigInteger relChord = Chord.relative(predecessorid, chordid);
        BigInteger relSelf = Chord.relative(predecessorid, self.getId());
        return relChord.signum() > 0 && relChord.compareTo(relSelf) <= 0;
    }
}
