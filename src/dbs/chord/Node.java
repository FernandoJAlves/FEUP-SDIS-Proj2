package dbs.chord;

import static dbs.chord.Chord.CHECK_PREDECESSOR_PERIOD;
import static dbs.chord.Chord.FIXFINGERS_PERIOD;
import static dbs.chord.Chord.NODE_DUMP_PERIOD;
import static dbs.chord.Chord.NODE_DUMP_TABLE;
import static dbs.chord.Chord.NODE_TASKS_POOL_SIZE;
import static dbs.chord.Chord.STABILIZE_PERIOD;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import dbs.chord.messages.AliveMessage;
import dbs.chord.messages.KeepAliveMessage;
import dbs.chord.messages.LookupMessage;
import dbs.chord.messages.PredecessorMessage;
import dbs.chord.messages.ResponsibleMessage;
import dbs.chord.messages.StabilizeMessage;
import dbs.chord.observers.AliveObserver;
import dbs.chord.observers.FixFingerObserver;
import dbs.chord.observers.JoinObserver;
import dbs.chord.observers.KeepAliveObserver;
import dbs.chord.observers.LookupObserver;
import dbs.chord.observers.PredecessorObserver;
import dbs.chord.observers.ResponsibleObserver;
import dbs.chord.observers.StabilizeObserver;
import dbs.network.SocketManager;

public class Node {

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
        this.pool = new ScheduledThreadPoolExecutor(NODE_TASKS_POOL_SIZE);
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
     * @return The NodeInfo data for this node's successor.
     */
    public NodeInfo getSuccessor() {
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
     *
     * @param chordId A file or node id whose successor (responsible) is to be found.
     * @return A promise that either resolves immediately to this node, or will
     *         resolve to the successor of chordId. If the network is unstable this
     *         lookup could fail.
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
     * * HANDLER: Handle generic LOOKUP message from the GetSuccessor subprotocol.
     */
    public void handleLookup(LookupMessage lookup) {
        // Are we responsible for this node?
        if (isResponsible(lookup.getChordId())) {
            ResponsibleMessage responsible = new ResponsibleMessage(lookup.getChordId());
            SocketManager.get().sendMessage(lookup.getSender(), responsible);
            return;
        }

        BigInteger chordId = lookup.getChordId();
        NodeInfo sourceNode = lookup.getSourceNode();
        LookupMessage newLookup = new LookupMessage(chordId, sourceNode);

        // Apparently we aren't, so forward the message to the closest preceding finger.
        NodeInfo closest = lookupClosestPrecedingFinger(chordId, newLookup);

        // Are we the closest preceding? Then check our successor. The message hasn't been sent yet.
        if (self.equals(closest)) {
            NodeInfo successor = finger.get(1);
            if (successor != null) {
                SocketManager.get().sendMessage(successor, newLookup);
            } else {
                System.err.println("Could not forward " + lookup + " as I do not have a successor");
            }
        }
    }

    /**
     * * HANDLER: Handle STABILIZE message from the Stabilize subprotocol.
     */
    public void handleStabilize(StabilizeMessage message) {
        NodeInfo sender = message.getSender();

        // If we don't have a predecessor, accept the given one.
        if (predecessor.get() == null) {
            predecessor.set(sender);
        }
        // If we have a predecessor, accept the sender if it is strictly better than the one we have now.
        else {
            BigInteger predecessorId = predecessor.get().getChordId();
            BigInteger senderId = sender.getChordId();
            BigInteger selfId = self.getChordId();

            if (Chord.strictOrdered(predecessorId, senderId, selfId)) {
                predecessor.set(sender);
            }
        }

        // Regardless of the result, answer the sender our predecessor now so he can be happy as well.
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

        if (self.getChordId().equals(candidateId) && predecessor.get() == null) {
            predecessor.set(response.getSender());
        } else if (Chord.strictOrdered(selfId, candidateId, successorId)) {
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
    public void handleFixFingerResponse(ResponsibleMessage response, int i) {
        NodeInfo responsible = response.getSender();
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
    public void handleJoinResponse(ResponsibleMessage response) {
        NodeInfo responsible = response.getSender();
        finger.set(1, responsible);
        System.out.println("Successfully joined network, with successor " + responsible);

        setupObservers();
    }

    /**
     * * HANDLER: Handle KEEPALIVE message from the CheckPredecessor subprotocol.
     */
    public void handleKeepAlive(KeepAliveMessage message) {
        NodeInfo sender = message.getSender();
        AliveMessage response = new AliveMessage();
        SocketManager.get().sendMessage(sender, response);
    }

    /**
     * * HANDLER: Handle ISALIVE message response for a KEEPALIVE message.
     */
    public void handleIsAlive(AliveMessage response) {
        // ok...
    }

    /**
     * * HANDLER: Handle a missing ISALIVE message response for a KEEPALIVE message,
     * * result of an observer timeout.
     */
    public void handleIsAliveTimeout() {
        System.out.println("Lost connection to predecessor: did not respond KEEPALIVE message");
        predecessor.set(null);

        // TODO: keep track of waiting predecessor.
    }

    /**
     * * HANDLER: Handle a missing RESPONSIBLE message response for a LOOKUP message
     * * serving as the join message for this node, result of an observer timeout.
     */
    public void handleFailedJoin() {
        System.out.println("Failed joining network");
    }

    /**
     * Create a new Chord network.
     */
    public void join() {
        System.out.println(self + " creating new Chord network");
        finger.set(1, self);

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
            if (fingerNode == null || self.getChordId().equals(fingerNode.getChordId()))
                continue;

            BigInteger fingerId = fingerNode.getChordId();

            if (Chord.strictOrdered(selfId, fingerId, chordId)) {
                boolean sent = SocketManager.get().sendMessage(fingerNode, message);

                if (sent) {
                    return fingerNode;
                } else {
                    System.err.println("Failed to send message to " + fingerNode);
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
        ChordDispatcher.get().addObserver(new KeepAliveObserver());
        ChordDispatcher.get().addObserver(new LookupObserver());

        BigInteger selfId = self.getChordId();
        for (int i = 1; i <= Chord.m; ++i)
            ChordDispatcher.get().addObserver(new FixFingerObserver(Chord.ithFinger(selfId, i), i));

        // Setup periodic runnables
        pool.scheduleWithFixedDelay(new Stabilize(), 500, STABILIZE_PERIOD, TimeUnit.MILLISECONDS);
        pool.scheduleWithFixedDelay(new FixFingers(), 500, FIXFINGERS_PERIOD, TimeUnit.MILLISECONDS);
        pool.scheduleWithFixedDelay(new CheckPredecessor(), 500, CHECK_PREDECESSOR_PERIOD, TimeUnit.MILLISECONDS);
        if (NODE_DUMP_TABLE)
            pool.scheduleWithFixedDelay(new Dump(), 650, NODE_DUMP_PERIOD, TimeUnit.MILLISECONDS);
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

        // The node should keep better track of which keys it is responsible for,
        // so this method will improve.

        return Chord.afterOrdered(predecessorNode.getChordId(), chordId, self.getChordId());
    }

    /**
     * Dump this Node's table to standard output.
     */
    public void dumpNode() {
        NodeInfo predecessorNode = predecessor.get();
        NodeInfo successorNode = finger.get(1);
        String predecessorStr = predecessorNode == null ? "?" : predecessorNode.toString();
        String successorStr = successorNode == null ? "?" : successorNode.toString();

        System.out.println("Table of " + self + " (id " + self.getChordId() + ")");
        System.out.println(" predecessor: " + predecessorStr);
        System.out.println(" successor:   " + successorStr);
        for (int i = 2; i <= Chord.m; ++i) {
            NodeInfo fingerNode = finger.get(i);
            String fingerStr = fingerNode == null ? "?" : fingerNode.toString();

            String minId = Chord.percentStr(Chord.ithFinger(self.getChordId(), i));
            System.out.println("  finger[" + i + "] (" + minId + "): " + fingerStr);
        }
        System.out.println();
    }

    /**
     * Periodically dump this node's fingers, predecessor and successor,
     * to see its view of the status of the Chord.
     */
    private class Dump implements Runnable {

        @Override
        public void run() {
            dumpNode();
        }
    }

    private class Stabilize implements Runnable {

        @Override
        public void run() {
            NodeInfo next = finger.get(1);
            if (next == null) {
                return;
            } else if (self.getChordId().equals(next.getChordId())) {
                next = predecessor.get();
                if (next != null)
                    finger.set(1, next);
            } else {
                StabilizeMessage update = new StabilizeMessage();
                SocketManager.get().sendMessage(next, update);
            }
        }
    }

    private class FixFingers implements Runnable {

        int i = 0;

        @Override
        public void run() {
            if (++i > Chord.m)
                i = 1;

            BigInteger selfId = self.getChordId();
            BigInteger fingerId = Chord.ithFinger(self.getChordId(), i);

            NodeInfo successor = finger.get(1);
            if (successor != null) {
                BigInteger successorId = successor.getChordId();

                if (Chord.afterOrdered(selfId, fingerId, successorId)) {
                    finger.set(i, successor);
                    return;
                }
            }

            LookupMessage lookup = new LookupMessage(fingerId, self);
            NodeInfo closest = lookupClosestPrecedingFinger(fingerId, lookup);

            if (self.getChordId().equals(closest.getChordId())) {
                finger.set(i, self); // ?
            } else {
                System.out.println("Looking for ith finger " + i + " " + fingerId + " in " + closest.shortStr());
            }
        }
    }

    private class CheckPredecessor implements Runnable {

        @Override
        public void run() {
            NodeInfo predecessorNode = predecessor.get();
            if (predecessorNode != null) {
                AliveObserver observer = new AliveObserver();
                KeepAliveMessage message = new KeepAliveMessage();
                ChordDispatcher.get().addObserver(observer);
                SocketManager.get().sendMessage(predecessorNode, message);
            }
        }
    }
}
