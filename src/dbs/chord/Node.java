package dbs.chord;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import dbs.chord.messages.Lookup;
import dbs.chord.messages.PredecessorResponse;
import dbs.chord.messages.PredecessorUpdate;
import dbs.chord.messages.Responsible;
import dbs.chord.observers.ResponsibleObserver;
import dbs.network.SocketManager;

public class Node {

    private final NodeServerInfo self;
    private final AtomicReference<NodeServerInfo> predecessor;
    private final AtomicReferenceArray<NodeServerInfo> finger;

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
        this.predecessor = new AtomicReference<>();
        this.finger = new AtomicReferenceArray<>(Chord.m + 1);
        instance = this;
    }

    public NodeServerInfo getServerInfo() {
        return self;
    }

    /**
     * Primary lookup interface. Returns a promise that will eventually resolve to
     * the node responsible for the given chordId, i.e. its successor.
     *
     * It may resolve to this node immediately, in which case the object's
     * getLocalAddress() will return null.
     */
    public CompletableFuture<NodeServerInfo> lookup(BigInteger chordId) {
        Lookup lookup = new Lookup(chordId, self);
        CompletableFuture<NodeServerInfo> promise = new CompletableFuture<>();
        ResponsibleObserver observer = new ResponsibleObserver(chordId, promise);

        BigInteger selfId = self.getChordId();

        for (int i = Chord.m; i > 0; --i) {
            NodeServerInfo next = finger.get(i);
            if (next == null)
                continue;

            BigInteger nextId = next.getChordId();

            if (Chord.compare(selfId, nextId, chordId) <= 0) {
                boolean sent = SocketManager.get().sendMessage(next, lookup);

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
        if (isResponsible(lookup.getChordId())) {
            Responsible responsible = new Responsible(lookup.getChordId());
            SocketManager.get().sendMessage(lookup.getSender(), responsible);
            return;
        }

        BigInteger chordId = lookup.getChordId();
        BigInteger selfId = self.getChordId();

        for (int i = Chord.m; i > 0; --i) {
            NodeServerInfo next = finger.get(i);
            if (next == null)
                continue;

            BigInteger nextId = next.getChordId();

            if (Chord.compare(selfId, nextId, chordId) <= 0) {
                boolean sent = SocketManager.get().sendMessage(next, lookup);
                if (sent)
                    return;
            }
        }

        System.err.println("Failed to find finger for lookup of " + lookup.getChordId());
    }

    public void predecessorUpdate() {
        PredecessorUpdate update = new PredecessorUpdate();
        SocketManager.get().sendMessage(finger.get(1), update);
    }

    public void handlePredecessorUpdate(PredecessorUpdate update) {
        NodeServerInfo sender = update.getSender();

        int c = Chord.compare(predecessor.get().getChordId(), sender.getChordId(), self.getChordId());
        if (c == -1) {
            predecessor.set(sender);
        }
        PredecessorResponse response = new PredecessorResponse(predecessor.get());
        SocketManager.get().sendMessage(sender, response);
    }

    public void handlePredecessorResponse(PredecessorResponse response) {
        NodeServerInfo candidate = response.getPredecessorNode();

        BigInteger candidateId = candidate.getChordId();
        BigInteger selfId = self.getChordId();
        BigInteger successorId = finger.get(1).getChordId();

        if (candidateId.compareTo(selfId) != 0 && Chord.compare(selfId, candidateId, successorId) == -1) {
            if (SocketManager.get().tryOpen(candidate)) {
                finger.set(1, candidate);
            } else {
                System.err.println("Could not connect to chosen candidate " + candidateId);
            }
        }
    }

    // 1 --> m
    void fixFinger(int fingerId) {
        BigInteger selfId = self.getChordId();
        BigInteger lowestFingerId = self.getChordId().add(BigInteger.TWO.shiftLeft(fingerId));

        Lookup lookup = new Lookup(lowestFingerId, self);

        for (int i = Chord.m; i > 0; --i) {
            NodeServerInfo next = finger.get(i);
            if (next == null)
                continue;

            BigInteger nextId = next.getChordId();

            if (Chord.compare(selfId, nextId, lowestFingerId) <= 0) {
                boolean sent = SocketManager.get().sendMessage(next, lookup);

                if (sent)
                    return;
            }
        }

        finger.set(i, self);
    }

    public void handleFixFinger(Responsible message, int i) {
        NodeServerInfo responsible = message.getSender();
        BigInteger responsibleId = responsible.getChordId();

        if (SocketManager.get().tryOpen(responsible)) {
            finger.set(1, responsible);
        } else {
            System.err.println("Could not connect to chosen responsible " + responsibleId + " of finger " + i);
        }
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
