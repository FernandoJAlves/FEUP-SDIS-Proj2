package dbs.chord;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import dbs.chord.messages.Lookup;
import dbs.chord.messages.PredecessorResponse;
import dbs.chord.messages.Responsible;
import dbs.chord.observers.ResponsibleObserver;
import dbs.network.SocketManager;

public class Node {

    private final NodeLocalInfo self;
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
        this.self = new NodeLocalInfo(nodeId, null, serverAddress);
        this.predecessor = new AtomicReference<>();
        this.finger = new AtomicReferenceArray<>(Chord.m);
        instance = this;
    }

    public NodeServerInfo getServerInfo() {
        return self.getServerInfo();
    }

    /**
     * Primary lookup interface. Returns a promise that will eventually resolve to
     * the node responsible for the given chordId, i.e. its successor.
     *
     * It may resolve to this node immediately, in which case the object's
     * getLocalAddress() will return null.
     */
    public CompletableFuture<NodeLocalInfo> lookup(BigInteger chordId) {
        Lookup lookup = new Lookup(chordId, self.getServerInfo());
        CompletableFuture<NodeLocalInfo> promise = new CompletableFuture<>();
        ResponsibleObserver observer = new ResponsibleObserver(chordId, promise);

        BigInteger selfId = self.getChordId();

        for (int i = Chord.m; i > 0; --i) {
            NodeLocalInfo next = finger.get(i);
            if (next == null)
                continue;

            BigInteger nextId = next.getChordId();

            if (Chord.compare(selfId, nextId, chordId) <= 0) {
                InetSocketAddress localAddress = next.getLocalAddress();
                boolean sent = SocketManager.get().sendMessage(localAddress, lookup);

                // Small race condition adding the observer. ***
                if (sent) {
                    ChordDispatcher.get().addObserver(observer);
                    return promise;
                }
            }
        }

        promise.complete(self);
        return promise;
    }

    /**
     * Primary lookup handling.
     */
    public void handleLookup(Lookup lookup) {
        BigInteger chordId = lookup.getChordId();
        BigInteger selfId = self.getChordId();

        for (int i = Chord.m; i > 0; --i) {
            NodeLocalInfo next = finger.get(i);
            if (next == null)
                continue;

            BigInteger nextId = next.getChordId();

            if (Chord.compare(selfId, nextId, chordId) <= 0) {
                InetSocketAddress localAddress = next.getLocalAddress();
                boolean sent = SocketManager.get().sendMessage(localAddress, lookup);
                if (sent)
                    return;
            }
        }

        Responsible responsible = new Responsible(chordId);
        InetSocketAddress sourceServerAddress = lookup.getSourceNode().getServerAddress();
        boolean opened = SocketManager.get().open(sourceServerAddress);
        // ^ no fix for this. SocketManager has to map IDs...
    }

    public void predecessorUpdate(NodeLocalInfo other) {
        int c = Chord.compare(predecessor.get().getChordId(), other.getChordId(), self.getChordId());
        if (c == -1) {
            predecessor.set(other);
        }
        PredecessorResponse response = new PredecessorResponse(predecessor.get().getServerInfo());
        SocketManager.get().sendMessage(other.getLocalAddress(), response);
    }

    /**
     *
     */
    private NodeLocalInfo lookupClosestPreceding(BigInteger chordId) {
        BigInteger selfId = self.getChordId();

        for (int i = Chord.m; i > 0; --i) {
            NodeLocalInfo next = finger.get(i);
            if (next == null)
                continue;

            BigInteger nextId = next.getChordId();

            if (Chord.compare(selfId, nextId, chordId) <= 0)
                return next;
        }

        return self;
    }

    /**
     * @return true if this Node is responsible for the given chord id.
     */
    private boolean isResponsible(BigInteger chordId) {
        // Return true if predecessorid -> chordId ==> selfid, which is:
        // 0 < relative(predecessorid, chordId) <= relative(predecessorid, selfid)
        BigInteger predecessorid = predecessor.get().getChordId();
        BigInteger relChord = Chord.relative(predecessorid, chordId);
        BigInteger relSelf = Chord.relative(predecessorid, self.getChordId());
        return relChord.signum() > 0 && relChord.compareTo(relSelf) <= 0;
    }
}
