package dbs.chord;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import dbs.chord.messages.LookupMessage;
import dbs.chord.messages.PredecessorMessage;
import dbs.chord.messages.ResponsibleMessage;
import dbs.chord.messages.StabilizeMessage;
import dbs.chord.observers.JoinObserver;
import dbs.chord.observers.LookupObserver;
import dbs.chord.observers.PredecessorObserver;
import dbs.chord.observers.ResponsibleObserver;
import dbs.chord.observers.StabilizeObserver;
import dbs.network.SocketManager;

public class Node {

    // Configuration
    private static final int CORE_POOL_SIZE = 2;
    private static final int STABILIZE_DELAY = 5000;
    private static final int FIXFINGERS_DELAY = 3000;
    private static final int CHECK_PREDECESSOR_DELAY = 4000;
    private static final int DUMP_DELAY = 10000;
    private static final boolean DUMP_NODE = true;

    private final NodeInfo self;
    private final AtomicReference<NodeInfo> predecessor;
    private final AtomicReferenceArray<NodeInfo> finger;

    private final ScheduledThreadPoolExecutor pool;

    private static Node instance;

    public static Node get() {
        return instance;
    }

    /**
     * Create the Node instance with the given server socket address.
     */
    public static Node create(InetSocketAddress serverAddress) {
        return new Node(serverAddress);
    }

    private Node(InetSocketAddress serverAddress) {
        assert instance == null;

        BigInteger nodeId = Chord.consistentHash(serverAddress);

        this.self = new NodeInfo(nodeId, serverAddress);
        this.predecessor = new AtomicReference<>();
        this.finger = new AtomicReferenceArray<>(Chord.m + 1);
        this.pool = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE);
        instance = this;
    }

    /**
     * @return The NodeInfo data for this node.
     */
    public NodeInfo getSelf() {
        return self;
    }

    /**
     * @return The NodeInfo data for this node's predecessor.
     */
    public NodeInfo getPredecessor() {
        return predecessor.get();
    }

    /**
     * @return The NodeInfo data for the ith finger of this node.
     */
    public NodeInfo getFinger(int fingerIndex) {
        return finger.get(fingerIndex);
    }

    /**
     * Primary lookup interface. Returns a promise that will eventually resolve to
     * the node responsible for the given chordId, i.e. its successor.
     *
     * It may resolve to this node immediately.
     */
    public CompletableFuture<NodeInfo> lookup(BigInteger chordId) {
        LookupMessage lookup = new LookupMessage(chordId, self);
        CompletableFuture<NodeInfo> promise = new CompletableFuture<>();
        ResponsibleObserver observer = new ResponsibleObserver(chordId, promise);

        if (isResponsible(chordId)) {
            promise.complete(self);
        } else if (lookupClosestPrecedingFinger(chordId, lookup).equals(self)) {
            promise.complete(null);
        } else {
            ChordDispatcher.get().addObserver(observer);
        }

        return promise;
    }

    /**
     * * HANDLER: Handle generic LOOKUP message.
     */
    public void handleLookup(LookupMessage lookup) {
        // Are we responsible for this node?
        if (isResponsible(lookup.getChordId())) {
            ResponsibleMessage responsible = new ResponsibleMessage(lookup.getChordId());
            SocketManager.get().sendMessage(lookup.getSender(), responsible);
            return;
        }

        BigInteger chordId = lookup.getChordId();

        NodeInfo target = lookupClosestPrecedingFinger(chordId, lookup);
        if (target.equals(self))
            System.err.println("Failed to find finger for lookup of " + lookup.getChordId());
    }

    /**
     * * HANDLER: Handle STABILIZE message.
     */
    public void handleStabilize(StabilizeMessage update) {
        NodeInfo sender = update.getSender();

        // If we don't have a predecessor, accept the given one.
        if (predecessor.get() == null) {
            predecessor.set(sender);
        } else {
            // If we have a predecessor, accept the sender if it is strictly better than the
            // one we have now.
            BigInteger predecessorId = predecessor.get().getChordId();
            BigInteger senderId = sender.getChordId();
            BigInteger selfId = self.getChordId();

            if (Chord.strictOrdered(predecessorId, senderId, selfId)) {
                predecessor.set(sender);
            }
        }

        // Regardless of the result, answer the sender our predecessor now.
        PredecessorMessage response = new PredecessorMessage(predecessor.get());
        SocketManager.get().sendMessage(sender, response);
    }

    /**
     * * HANDLER: Handle PREDECESSOR message, response to a STABILIZE message.
     */
    public void handlePredecessor(PredecessorMessage response) {
        // If the sender is not our successor, discard and continue.
        if (!response.getSender().equals(finger.get(1)))
            return;

        NodeInfo candidate = response.getPredecessorNode();

        BigInteger candidateId = candidate.getChordId();
        BigInteger selfId = self.getChordId();
        BigInteger successorId = finger.get(1).getChordId();

        if (Chord.strictOrdered(selfId, candidateId, successorId)) {
            if (SocketManager.get().tryOpen(candidate)) {
                finger.set(1, candidate);
            } else {
                System.err.println("Could not connect to chosen, valid candidate successor " + candidateId);
            }
        }
    }

    /**
     * * HANDLER: Handle RESPONSIBLE message for a fix finger lookup.
     */
    public void handleFixFingerResponse(ResponsibleMessage message, int i) {
        NodeInfo responsible = message.getSender();
        BigInteger responsibleId = responsible.getChordId();

        if (SocketManager.get().tryOpen(responsible)) {
            finger.set(i, responsible);
        } else {
            System.err.println("Could not connect to chosen, valid responsible " + responsibleId + " of finger " + i);
        }
    }

    /**
     * * HANDLER: Handle RESPONSIBLE message for a join lookup.
     */
    public void handleJoinResponse(ResponsibleMessage message) {
        finger.set(1, message.getSender());
        System.out.println("Successfully joined network, with successor " + message.getSender());

        setupObservers();
    }

    /**
     * Create a new Chord network.
     */
    public void join() {
        System.out.println(self + " creating new Chord network");

        setupObservers();
    }

    /**
     * Try to join an existing Chord network through the given remote node's
     * interface.
     */
    public void join(NodeInfo remoteNode) {
        System.out.println(self + " joining Chord on remote node " + remoteNode);

        JoinObserver joiner = new JoinObserver();
        ChordDispatcher.get().addObserver(joiner);

        LookupMessage lookup = new LookupMessage(self.getChordId(), self);
        SocketManager.get().sendMessage(remoteNode, lookup);
    }

    /**
     * Send the given lookup to the closest preceding finger to the given chord id.
     * If no such finger exists or can be contacted, returns self.
     */
    private NodeInfo lookupClosestPrecedingFinger(BigInteger chordId, LookupMessage message) {
        BigInteger selfId = self.getChordId();

        for (int i = Chord.m; i > 0; --i) {
            NodeInfo fingerNode = finger.get(i);
            if (fingerNode == null || fingerNode.equals(self))
                continue;

            BigInteger fingerId = fingerNode.getChordId();

            if (Chord.afterOrdered(selfId, fingerId, chordId)) {
                boolean sent = SocketManager.get().sendMessage(fingerNode, message);

                if (sent) {
                    return fingerNode;
                } else {
                    finger.set(i, null);
                }
            }
        }

        return self;
    }

    /**
     * Launch observers and schedule protocol's periodic tasks.
     */
    private void setupObservers() {
        // Setup permanent observers
        ChordDispatcher.get().addObserver(new StabilizeObserver());
        ChordDispatcher.get().addObserver(new PredecessorObserver());
        ChordDispatcher.get().addObserver(new LookupObserver());

        // Setup periodic runnables
        pool.scheduleWithFixedDelay(new Stabilize(), 500, STABILIZE_DELAY, TimeUnit.MILLISECONDS);
        pool.scheduleWithFixedDelay(new FixFingers(), 500, FIXFINGERS_DELAY, TimeUnit.MILLISECONDS);
        pool.scheduleWithFixedDelay(new CheckPredecessor(), 500, CHECK_PREDECESSOR_DELAY, TimeUnit.MILLISECONDS);
        if (DUMP_NODE)
            pool.scheduleWithFixedDelay(new Dump(), 500, DUMP_DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * @return true if this Node is responsible for the given chord id.
     */
    private boolean isResponsible(BigInteger chordId) {
        // PredecessorMessage is null if this node is the first node.
        // Assume no predecessor fails for now.
        NodeInfo predecessorNode = predecessor.get();
        if (predecessorNode == null)
            return true;

        return Chord.afterOrdered(predecessorNode.getChordId(), chordId, self.getChordId());
    }

    /**
     * Dump this Node's table to standard output.
     */
    public void dumpNode() {
        System.out.println("Table of " + self);
        System.out.println(" predecessor: " + (predecessor.get() == null ? "?" : predecessor.get()));
        System.out.println(" successor:   " + (finger.get(1) == null ? "?" : finger.get(1)));
        for (int i = 1; i <= Chord.m; ++i) {
            System.out.println("  finger[" + i + "]: " + (finger.get(i) == null ? "?" : finger.get(i)));
        }
        System.out.println();
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
            assert finger.get(1) != null;

            if (!self.equals(finger.get(1))) {
                StabilizeMessage update = new StabilizeMessage();
                SocketManager.get().sendMessage(finger.get(1), update);
            }
        }
    }

    private class FixFingers implements Runnable {

        int fingerIndex = 0;

        @Override
        public void run() {
            if (++fingerIndex == (Chord.m + 1))
                fingerIndex = 1;

            BigInteger selfId = self.getChordId();
            BigInteger fingerId = Chord.ithFinger(self.getChordId(), fingerIndex);

            LookupMessage lookup = new LookupMessage(fingerId, self);

            for (int i = Chord.m; i > 0; --i) {
                NodeInfo next = finger.get(i);
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
    }

    private class CheckPredecessor implements Runnable {

        @Override
        public void run() {
            // ...
        }
    }
}
