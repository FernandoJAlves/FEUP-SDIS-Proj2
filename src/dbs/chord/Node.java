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
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.GetPredecessorMessage;
import dbs.chord.messages.KeepAliveMessage;
import dbs.chord.messages.LookupMessage;
import dbs.chord.messages.NotifyMessage;
import dbs.chord.messages.PredecessorMessage;
import dbs.chord.messages.ResponsibleMessage;
import dbs.chord.observers.AliveObserver;
import dbs.chord.observers.FixFingerObserver;
import dbs.chord.observers.GetPredecessorObserver;
import dbs.chord.observers.JoinObserver;
import dbs.chord.observers.KeepAliveObserver;
import dbs.chord.observers.LookupObserver;
import dbs.chord.observers.NotifyObserver;
import dbs.chord.observers.PredecessorObserver;
import dbs.chord.observers.ResponsibleObserver;
import dbs.network.SocketManager;

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

        ChordLogger.logNodeImportant("Created " + self);

        setupPermanentObservers();
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
     * It may resolve to this node immediately if this node is the sought successor,
     * and may resolve to null (leaking) if the successor could not be found or contacted.
     *
     * @param chordId A file or node id whose successor (responsible) is to be found.
     * @return A promise that either resolves immediately to this node, or will resolve
     *         to the successor of chordId. If the network is unstable this lookup
     *         could fail, and resolve to null either immediately or after a timeout.
     */
    public CompletableFuture<NodeInfo> lookup(BigInteger chordId) {
        LookupMessage lookup = new LookupMessage(chordId, self);
        CompletableFuture<NodeInfo> promise = new CompletableFuture<>();
        ResponsibleObserver observer = new ResponsibleObserver(chordId, promise);

        if (isResponsible(chordId)) {
            promise.complete(self);
        } else if (lookupClosestPreceding(chordId, lookup) == null) {
            promise.complete(null);
        } else {
            ChordDispatcher.get().addObserver(observer);
        }

        return promise;
    }

    /**
     * * HANDLER: Handle any LOOKUP message from the GetSuccessor subprotocol.
     */
    public void handleLookup(LookupMessage lookup) {
        BigInteger chordId = lookup.getChordId();
        NodeInfo sourceNode = lookup.getSourceNode();

        // Are we responsible for this node?
        if (isResponsible(chordId)) {
            ResponsibleMessage responsible = new ResponsibleMessage(chordId);
            SocketManager.get().sendMessage(sourceNode, responsible);
            return;
        }

        // We aren't, so we want to forward the message.
        LookupMessage newLookup = new LookupMessage(chordId, sourceNode);

        // The message will not be forwarded if we are the closest preceding
        // and our successor is the responsible node.
        // S    --> --> --> -->   N0   --> -->   Us   ->  ChordId  ->  Our Successor
        if (lookupClosestPreceding(chordId, newLookup) == null) {
            NodeInfo successorNode = finger.get(1);

            if (successorNode != null && Chord.afterOrdered(self.getChordId(), chordId, successorNode.getChordId())) {
                SocketManager.get().sendMessage(successorNode, newLookup);
            } else if (successorNode == null) {
                ChordLogger.nodeError("Could not forward " + lookup + " as I do not have a successor");
            }
        }
    }

    /**
     * * HANDLER: Handle STABILIZE message from the Stabilize subprotocol.
     */
    public void handleGetPredecessor(GetPredecessorMessage message) {
        NodeInfo senderNode = message.getSender();
        NodeInfo predecessorNode = predecessor.compareAndExchange(null, senderNode);

        if (predecessorNode == null) {
            ChordLogger.logNodeImportant("New predecessor: " + senderNode.shortStr());
            predecessorNode = senderNode;
        }

        PredecessorMessage response = new PredecessorMessage(predecessorNode);
        SocketManager.get().sendMessage(senderNode, response);
    }

    /**
     * * HANDLER: Handle PREDECESSOR message, response to a STABILIZE message.
     */
    public void handlePredecessor(PredecessorMessage response) {
        NodeInfo successorNode = finger.get(1);
        NodeInfo senderNode = response.getSender();

        // If the sender is not our successor, discard and continue.
        if (!response.getSender().equals(successorNode))
            return;

        NodeInfo candidateNode = response.getPredecessorNode();
        BigInteger candidateId = candidateNode.getChordId();
        BigInteger selfId = self.getChordId();
        BigInteger successorId = successorNode.getChordId();

        if (selfId.equals(candidateId)) {
            if (predecessor.compareAndSet(null, senderNode)) {
                ChordLogger.logNodeImportant("New predecessor: " + senderNode.shortStr() + " (adopted successor)");
            }
        } else if (Chord.strictOrdered(selfId, candidateId, successorId)) {
            if (SocketManager.get().tryOpen(candidateNode)) {
                if (finger.compareAndSet(1, successorNode, candidateNode)) {
                    ChordLogger.logNodeImportant("New successor: " + senderNode);
                }
            } else {
                ChordLogger.nodeError("Could not connect to chosen, valid candidate successor " + candidateId);
            }
        }

        NotifyMessage message = new NotifyMessage();
        assertSend(finger.get(1), message);
    }

    public void handleNotify(NotifyMessage message) {
        NodeInfo senderNode = message.getSender();
        NodeInfo predecessorNode = predecessor.compareAndExchange(null, senderNode);

        if (predecessorNode == null) {
            ChordLogger.logNodeImportant("New predecessor: " + senderNode.shortStr());
            predecessorNode = senderNode;
        } else {
            BigInteger predecessorId = predecessorNode.getChordId();
            BigInteger senderId = senderNode.getChordId();
            BigInteger selfId = self.getChordId();

            if (Chord.strictOrdered(predecessorId, senderId, selfId)) {
                if (predecessor.compareAndSet(predecessorNode, senderNode)) {
                    ChordLogger.logNodeImportant("New predecessor: " + senderNode.shortStr());
                }
            }
        }
    }

    /**
     * * HANDLER: Handle RESPONSIBLE message for a fix finger lookup.
     */
    public void handleFixFingerResponse(ResponsibleMessage response, int i) {
        NodeInfo responsibleNode = response.getSender();
        BigInteger responsibleId = responsibleNode.getChordId();

        if (SocketManager.get().tryOpen(responsibleNode)) {
            finger.set(i, responsibleNode);
        } else {
            ChordLogger.nodeError("Could not connect to chosen valid responsible " + responsibleId + " of finger " + i);
        }
    }

    /**
     * * HANDLER: Handle RESPONSIBLE message for a join lookup.
     */
    public void handleJoinResponse(ResponsibleMessage response) {
        NodeInfo responsibleNode = response.getSender();

        ChordLogger.logNodeImportant("New successor: " + responsibleNode.shortStr());
        finger.set(1, responsibleNode);

        ChordLogger.logNodeImportant("Successfully joined network");

        setupSubprotocols();
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
        // ok... lol
    }

    /**
     * * HANDLER: Handle a missing ISALIVE message response for a KEEPALIVE message,
     * * result of an observer timeout.
     */
    public void handleIsAliveTimeout(NodeInfo waitedNode) {
        if (waitedNode.equals(predecessor.get())) {
            ChordLogger.logNodeImportant("Lost connection to predecessor: did not respond KEEPALIVE request");
            predecessor.set(null);
        } else {
            ChordLogger.logNode("IsAlive timeout discarded: predecessor changed (not " + waitedNode.shortStr() + ")");
        }
    }

    /**
     * * HANDLER: Handle a missing RESPONSIBLE message response for a LOOKUP message
     * * serving as the join message for this node, result of an observer timeout.
     */
    public void handleFailedJoin() {
        ChordLogger.nodeError("Could not connect to Chord network: timeout occurred");
    }

    /**
     * Called by the frontend to have this Node create a new Chord network.
     */
    public void join() {
        ChordLogger.logNodeImportant(self + " creating new Chord network");
        finger.set(1, self);

        setupSubprotocols();
    }

    /**
     * Called by the frontend to have this Node join an already existing Chord network
     * through the given remote node.
     */
    public void join(NodeInfo remoteNode) {
        ChordLogger.logNodeImportant("Joining Chord on remote " + remoteNode);

        JoinObserver joiner = new JoinObserver();
        ChordDispatcher.get().addObserver(joiner);

        LookupMessage lookup = new LookupMessage(self.getChordId(), self);
        SocketManager.get().sendMessage(remoteNode, lookup);
    }

    /**
     * Send the given lookup message to the closest preceding finger F to the given chord id.
     * If no such F exists or can be contacted, returns null.
     * Otherwise returns F, who the message was sent to.
     * If the finger F has the exact id that is being looked up, it gets the message.
     *
     * A caller can tell the message was sent if the return value is not null.
     *
     * @param chordId A chord id.
     * @param message A lookup message to send to the closest finger found.
     * @param self The finger the message was sent to, or null if no such finger was found.
     */
    private NodeInfo lookupClosestPreceding(BigInteger chordId, LookupMessage message) {
        BigInteger selfId = self.getChordId();

        for (int i = Chord.m; i > 0; --i) {
            NodeInfo fingerNode = finger.get(i);
            if (fingerNode == null)
                continue;

            BigInteger fingerId = fingerNode.getChordId();
            if (selfId.equals(fingerId))
                continue;

            if (Chord.afterOrdered(selfId, fingerId, chordId)) {
                boolean sent = assertSend(fingerNode, message);

                if (sent) {
                    ChordLogger.logNode("Message to " + chordId + " sent closest preceding finger " + i + " "
                            + fingerNode.shortStr());
                    return fingerNode;
                }
            }
        }

        ChordLogger.logNode("No closest preceding finger for " + chordId);
        return null;
    }

    /**
     * Launch observers and schedule protocol's periodic tasks.
     */
    private void setupPermanentObservers() {
        ChordDispatcher.get().addObserver(new GetPredecessorObserver());
        ChordDispatcher.get().addObserver(new PredecessorObserver());
        ChordDispatcher.get().addObserver(new KeepAliveObserver());
        ChordDispatcher.get().addObserver(new LookupObserver());
        ChordDispatcher.get().addObserver(new NotifyObserver());

        BigInteger selfId = self.getChordId();
        for (int i = 1; i <= Chord.m; ++i)
            ChordDispatcher.get().addObserver(new FixFingerObserver(Chord.ithFinger(selfId, i), i));

        ChordLogger.logNode("Setup permanent observers");
    }

    private void setupSubprotocols() {
        pool.scheduleWithFixedDelay(new Stabilize(), 100, STABILIZE_PERIOD, TimeUnit.MILLISECONDS);
        pool.scheduleWithFixedDelay(new FixFingers(), 500, FIXFINGERS_PERIOD, TimeUnit.MILLISECONDS);
        pool.scheduleWithFixedDelay(new CheckPredecessor(), 300, CHECK_PREDECESSOR_PERIOD, TimeUnit.MILLISECONDS);
        if (NODE_DUMP_TABLE)
            pool.scheduleWithFixedDelay(new Dump(), 1700, NODE_DUMP_PERIOD, TimeUnit.MILLISECONDS);

        ChordLogger.logNode("Setup subprotocol tasks");
    }

    /**
     * @return true if this Node is responsible for the given chord id.
     */
    private boolean isResponsible(BigInteger chordId) {
        NodeInfo predecessorNode = predecessor.get();
        if (predecessorNode == null) {
            return self.equals(finger.get(1));
        }

        // The node should keep better track of which keys it is responsible for,
        // so this method will improve.
        BigInteger predecessorId = predecessorNode.getChordId();
        BigInteger selfId = self.getChordId();

        return Chord.afterOrdered(predecessorId, chordId, selfId);
    }

    /**
     * Dump this Node's table to standard output.
     */
    public void dumpNode() {
        NodeInfo predecessorNode = predecessor.get();
        NodeInfo successorNode = finger.get(1);
        String predecessorStr = Chord.print(predecessorNode);
        String successorStr = Chord.print(successorNode);

        System.out.println("\nTable of " + self + " (id " + self.getChordId() + ")");
        System.out.println(" predecessor: " + predecessorStr);
        System.out.println(" successor:   " + successorStr);
        for (int i = 2; i <= Chord.m; ++i) {
            NodeInfo fingerNode = finger.get(i);
            String fingerStr = Chord.print(fingerNode);

            String minId = Chord.percentStr(Chord.ithFinger(self.getChordId(), i));
            System.out.println("  finger[" + i + "] (" + minId + "): " + fingerStr);
        }
        System.out.println();
    }

    /**
     * Try to send a message to the given node, or else remove any trace of connection to it.
     */
    private boolean assertSend(NodeInfo remoteNode, ChordMessage message) {
        boolean sent = SocketManager.get().sendMessage(remoteNode, message);
        if (!sent) {
            ChordLogger.logNode("Purging " + remoteNode + ": disconnected");

            NodeInfo predecessorNode = predecessor.get();
            if (remoteNode.equals(predecessorNode)) {
                predecessor.compareAndSet(predecessorNode, null);
            }

            for (int i = 1; i <= Chord.m; ++i) {
                if (remoteNode.equals(finger.get(i))) {
                    finger.compareAndSet(i, remoteNode, null);
                }
            }
        }
        return sent;
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
            NodeInfo successor = finger.get(1), next = successor;
            if (next == null) {
                ChordLogger.logStabilize("bad: no successor");
                return;
            } else if (self.getChordId().equals(next.getChordId())) {
                next = predecessor.get();
                if (next != null) {
                    if (finger.compareAndSet(1, successor, next)) {
                        ChordLogger.logNodeImportant("New successor: adopt predecessor " + next.shortStr());
                    }
                } else {
                    ChordLogger.logStabilize("self successor, no predecessor");
                }
            } else {
                ChordLogger.logStabilize("challenge");
                GetPredecessorMessage message = new GetPredecessorMessage();
                assertSend(next, message);
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

            if (isResponsible(fingerId)) {
                ChordLogger.logFixFingers(i, fingerId, "responsible");
                finger.set(i, self);
                return;
            }

            NodeInfo successorNode = finger.get(1);
            if (successorNode != null) {
                BigInteger successorId = successorNode.getChordId();

                if (Chord.afterOrdered(selfId, fingerId, successorId)) {
                    ChordLogger.logFixFingers(i, fingerId, "successor");
                    finger.set(i, successorNode);
                    return;
                }
            }

            LookupMessage message = new LookupMessage(fingerId, self);
            NodeInfo destination = lookupClosestPreceding(fingerId, message);

            if (destination == null) {
                ChordLogger.logFixFingers(i, fingerId, "self");
                finger.set(i, self);
            } else {
                ChordLogger.logFixFingers(i, fingerId, "lookup " + destination.shortStr());
            }
        }
    }

    private class CheckPredecessor implements Runnable {

        @Override
        public void run() {
            NodeInfo predecessorNode = predecessor.get();
            if (predecessorNode != null) {
                ChordLogger.logCheckPredecessor("keep alive");
                AliveObserver observer = new AliveObserver(predecessorNode);
                KeepAliveMessage message = new KeepAliveMessage();
                ChordDispatcher.get().addObserver(observer);
                SocketManager.get().sendMessage(predecessorNode, message);
            } else {
                ChordLogger.logCheckPredecessor("no predecessor");
            }
        }
    }
}
