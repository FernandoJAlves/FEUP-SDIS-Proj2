package dbs.chord.messages;

import java.util.LinkedList;
import dbs.chord.Node;
import dbs.chord.NodeInfo;

public abstract class ForwardingMessage extends ChordMessage {

    private final LinkedList<NodeInfo> visitors;

    protected ForwardingMessage(ForwardingMessage message) {
        super(message.getKey());
        this.visitors = new LinkedList<>(message.visitors);
        visit(Node.get().getSelf());
    }

    protected ForwardingMessage(ChordMessageKey key) {
        super(key);
        this.visitors = new LinkedList<>();
        visit(Node.get().getSelf());
    }

    protected ForwardingMessage(String kind) {
        super(kind);
        this.visitors = new LinkedList<>();
        visit(Node.get().getSelf());
    }

    public final void visit(NodeInfo node) {
        assert !visitors.contains(node);
        visitors.add(node);
    }

    public final boolean visited(NodeInfo node) {
        return visitors.contains(node);
    }

    public final LinkedList<NodeInfo> getVisitors() {
        return new LinkedList<NodeInfo>(visitors);
    }

    public final int getNumVisits() {
        return visitors.size();
    }
}
