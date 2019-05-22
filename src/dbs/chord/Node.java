package dbs.chord;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class Node {

    private final NodeServerInfo self;
    private final NodeLocalInfo localSelf;
    private final AtomicReference<NodeLocalInfo> predecessor;
    private final AtomicReferenceArray<NodeLocalInfo> finger;

    private static Node instance;

    public static Node get() {
        return instance;
    }

    public static Node create(InetSocketAddress serverAddress) {
        return new Node(serverAddress);
    }

    Node(InetSocketAddress serverAddress) {
        assert instance == null;
        BigInteger nodeId = Chord.consistentHash(serverAddress);
        this.self = new NodeServerInfo(nodeId, serverAddress);
        this.localSelf = new NodeLocalInfo(nodeId, null);
        this.predecessor = new AtomicReference<>();
        this.finger = new AtomicReferenceArray<>(Chord.m);
        instance = this;
    }

    /**
     * Primary lookup interface. Returns a promise that will eventually resolve to
     * the node responsible for the given chordid, i.e. its successor.
     */
    public CompletableFuture<NodeLocalInfo> lookup(BigInteger chordid) {
        // If this node is responsible, return ourselves.
        if (isResponsible(chordid)) {
            return CompletableFuture.completedFuture(localSelf);
        }

        return null;
    }

    /**
     *
     */
    private NodeLocalInfo lookupClosestPreceding(BigInteger chordid) {
        BigInteger selfId = self.getChordId();

        for (int i = Chord.m; i > 0; --i) {
            NodeLocalInfo next = finger.get(i);
            if (next == null)
                continue;

            BigInteger nextId = next.getChordId();

            if (Chord.compare(selfId, nextId, chordid) <= 0)
                return next;
        }

        return localSelf;
    }

    /**
     * @return true if this Node is responsible for the given chord id.
     */
    private boolean isResponsible(BigInteger chordid) {
        // Return true if predecessorid -> chordid ==> selfid, which is:
        // 0 < relative(predecessorid, chordid) <= relative(predecessorid, selfid)
        BigInteger predecessorid = predecessor.get().getChordId();
        BigInteger relChord = Chord.relative(predecessorid, chordid);
        BigInteger relSelf = Chord.relative(predecessorid, self.getChordId());
        return relChord.signum() > 0 && relChord.compareTo(relSelf) <= 0;
    }
}
