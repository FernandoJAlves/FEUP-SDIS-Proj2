package dbs.chord;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Iterator;

import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.observers.ChordObserver;

public final class ChordObserverList {

    private final ChordMessageKey key;
    private final ArrayDeque<ChordObserver> observers;

    ChordObserverList(ChordMessageKey key) {
        this.key = key;
        this.observers = new ArrayDeque<>();
    }

    synchronized void add(ChordObserver observer) {
        this.observers.add(observer);
    }

    synchronized void dispatch(ChordMessage message, InetSocketAddress localAddress) {
        Iterator<ChordObserver> iterator = observers.descendingIterator();

        NodeLocalInfo info = new NodeLocalInfo(message.getSender(), localAddress);

        while (iterator.hasNext()) {
            ChordObserver observer = iterator.next();
            if (observer.notify(message, info))
                iterator.remove();
        }
    }
}
