package dbs.chord;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import dbs.chord.messages.Lookup;
import dbs.chord.messages.PredecessorResponse;
import dbs.chord.messages.PredecessorUpdate;
import dbs.chord.messages.Responsible;
import dbs.chord.observers.JoinObserver;
import dbs.chord.observers.LookupObserver;
import dbs.chord.observers.PredecessorResponseObserver;
import dbs.chord.observers.PredecessorUpdateObserver;
import dbs.chord.observers.ResponsibleObserver;
import dbs.network.SocketManager;

public class Node {

    // Milliseconds time unit
    private final int STABILIZE_DELAY = 5000;
    private final int FIXFINGERS_DELAY = 3000;
    private final int CHECK_PREDECESSOR_DELAY = 4000;
    private final int DUMP_DELAY = 10000;

    private final NodeServerInfo self;
    private final AtomicReference<NodeServerInfo> predecessor;
    private final AtomicReferenceArray<NodeServerInfo> finger;

    private Stabilize stabilize;
    private FixFingers fixFingers;
    private ScheduledThreadPoolExecutor pool;

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

    public NodeServerInfo getSelf() {
        return self;
    }

    public NodeServerInfo getPredecessor() {
        return predecessor.get();
    }

    public NodeServerInfo getFinger(int fingerIndex) {
        return finger.get(fingerIndex);
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

    public void stabilize() {
        assert finger.get(1) != null;

        if (!self.equals(finger.get(1))) {
            PredecessorUpdate update = new PredecessorUpdate();
            SocketManager.get().sendMessage(finger.get(1), update);
        }
    }

    public void handlePredecessorUpdate(PredecessorUpdate update) {
        NodeServerInfo sender = update.getSender();

        // initial update.
        if (predecessor.get() == null) {
            predecessor.set(sender);
        } else {
            int c = Chord.compare(predecessor.get().getChordId(), sender.getChordId(), self.getChordId());
            if (c == -1) {
                predecessor.set(sender);
            }
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
    public void fixFinger(int fingerIndex) {
        BigInteger selfId = self.getChordId();
        BigInteger fingerId = Chord.ithFinger(self.getChordId(), fingerIndex);

        Lookup lookup = new Lookup(fingerId, self);

        for (int i = Chord.m; i > 0; --i) {
            NodeServerInfo next = finger.get(i);
            if (next == null)
                continue;

            BigInteger nextId = next.getChordId();
            if (nextId.equals(selfId))
                break;

            if (Chord.compare(selfId, nextId, fingerId) <= 0) {
                boolean sent = SocketManager.get().sendMessage(next, lookup);

                if (sent)
                    return;
            }
        }

        finger.set(fingerIndex, self);
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
     * Create a new Chord network.
     */
    public void join() {
        System.out.println(self + " creating new Chord network");

        // predecessor.set(null); // IMPLICIT

        setupObservers();
    }

    public void join(NodeServerInfo remoteNode) {
        System.out.println(self + " joining Chord on remote node " + remoteNode);

        // predecessor.set(null); // IMPLICIT

        JoinObserver joiner = new JoinObserver();
        ChordDispatcher.get().addObserver(joiner);

        Lookup lookup = new Lookup(self.getChordId(), self);
        SocketManager.get().sendMessage(remoteNode, lookup);
    }

    public void resolveSuccessor(Responsible message) {
        finger.set(1, message.getSender());
        System.out.println("Fuck yea");

        setupObservers();
    }

    private void setupObservers() {
        // Setup permanent observers
        PredecessorUpdateObserver updater = new PredecessorUpdateObserver();
        PredecessorResponseObserver responder = new PredecessorResponseObserver();
        LookupObserver lookuper = new LookupObserver();
        ChordDispatcher.get().addObserver(updater);
        ChordDispatcher.get().addObserver(responder);
        ChordDispatcher.get().addObserver(lookuper);

        // Setup periodic runnables and pool
        this.stabilize = new Stabilize();
        this.fixFingers = new FixFingers();

        this.pool = new ScheduledThreadPoolExecutor(2);
        this.pool.scheduleWithFixedDelay(stabilize, 500, STABILIZE_DELAY, TimeUnit.MILLISECONDS);
        this.pool.scheduleWithFixedDelay(fixFingers, 500, FIXFINGERS_DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * @return true if this Node is responsible for the given chord id.
     */
    private boolean isResponsible(BigInteger chordId) {
        // Predecessor is null if this node is the first node.
        if (predecessor.get() == null)
            return true;

        // Return true if predecessorid -> chordId ==> selfid, which is:
        // 0 < relative(predecessorid, chordId) <= relative(predecessorid, selfid)
        BigInteger predecessorid = predecessor.get().getChordId();
        BigInteger relChord = Chord.relative(predecessorid, chordId);
        BigInteger relSelf = Chord.relative(predecessorid, self.getChordId());
        return relChord.signum() > 0 && relChord.compareTo(relSelf) <= 0;
    }

    public void dumpNode() {
        System.out.println("Dump of " + self);
        System.out.println(" predecessor: " + (predecessor.get() == null ? "?" : predecessor.get()));
        System.out.println(" successor:   " + (finger.get(1) == null ? "?" : finger.get(1)));
        // for (int i = 1; i <= Chord.m; ++i) {
        // System.out.println(" finger[" + i + "]: " + (finger.get(i) == null ? "?" :
        // finger.get(i)));
        // }
    }

    private class Dump implements Runnable {

        @Override
        public void run() {
            dumpNode();
        }
    }

    private class Stabilize implements Runnable {

        @Override
        public void run() {
            stabilize();
        }
    }

    private class FixFingers implements Runnable {

        int fingerIndex = 0;

        @Override
        public void run() {
            if (++fingerIndex == (Chord.m + 1))
                fingerIndex = 1;
            fixFinger(fingerIndex);
        }
    }

    private class CheckPredecessor implements Runnable {

        @Override
        public void run() {
            checkPredecessor();
        }
    }
}
