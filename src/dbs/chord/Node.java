package dbs.chord;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class Node {

    private final NodeInfo self;
    private final AtomicReference<NodeInfo> predecessor;
    private final AtomicReferenceArray<NodeInfo> finger;

    private static Node instance;

    public static Node get() {
        return instance;
    }

    Node(NodeInfo self) {
        this.self = self;
        this.predecessor = new AtomicReference<>();
        this.finger = new AtomicReferenceArray<>(16);
    }

    public CompletableFuture<NodeInfo> lookup(BigInteger chordid) {
        if (isResponsible(chordid)) {
            return CompletableFuture.completedFuture(self);
        }

        for
    }

    private NodeInfo closestPrecedingInTable(BigInteger chordid) {
        for (int i = finger.length; i > 0; --i) {
            NodeInfo next = finger.get(i);
            if (next.getId().compareTo(chordid) <= 0)
                return next;
        }
    }

    /**
     * @return true if this Node is responsible for the given chord id.
     */
    private boolean isResponsible(BigInteger chordid) {
        BigInteger predecessorid = predecessor.get().getId();
        BigInteger selfid = self.getId();
        return predecessorid.compareTo(chordid) < 0 && selfid.compareTo(chordid) >= 0;
    }

    /**
     * Compute lhs - rhs modulo 2^m
     */
    private BigInteger relative(BigInteger lhs, BigInteger rhs) {

    }
}
