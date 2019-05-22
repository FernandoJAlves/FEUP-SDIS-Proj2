package dbs.chord;

import java.util.concurrent.ConcurrentHashMap;

import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.observers.ChordObserver;

public final class ChordDispatcher {

    private final ConcurrentHashMap<ChordMessageKey, ChordObserverList> observerMap;

    private static final ChordDispatcher instance = new ChordDispatcher();

    public static ChordDispatcher get() {
        return instance;
    }

    private ChordDispatcher() {
        this.observerMap = new ConcurrentHashMap<>();
    }

    public void addObserver(ChordObserver observer) {
        ChordMessageKey key = observer.getSubscribedKey();
        observerMap.computeIfAbsent(key, ChordObserverList::new).add(observer);
    }

    public void dispatch(ChordMessage message) {
        ChordMessageKey key = message.getKey();
        if (key != null) {
            ChordObserverList list = observerMap.get(key);
            System.out.println("Received message with key " + key);

            if (list != null) {
                list.dispatch(message);
            } else {
                System.err.println("Unexpected: no observer list waiting on messages " + key);
            }
        } else {
            System.err.println("Received message with null key");
        }
    }
}
