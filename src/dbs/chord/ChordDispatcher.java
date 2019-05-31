package dbs.chord;

import static dbs.chord.Chord.DISPATCHER_TASKS_POOL_SIZE;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.observers.ChordObserver;

/**
 * The ChordDispatcher singleton is our implementation of the subscriber (or
 * observer) pattern which is used to handle messages received from the
 * ChordListeners which are listening in on the open sockets.
 *
 * The traditional subscriber pattern holds a mutable collection of objects
 * which would like to be 'notified' when the state of an object changes. In our
 * case, the Chord Observers will subscribe on a Key (ChordMessageKey) which is
 * contained in every ChordMessage instance. Once a message with that key
 * arrives, the subscribers will be notified with the message plain.
 *
 * The same Observer can subscribe multiple different keys, but notifications are
 * on a per-key basis.
 *
 * There are three kinds of subscribers:
 *
 * * Permanent subscribers: these subscribers are never removed and never
 * * timeout. They will remain until explicitly removed or until the Node
 * * terminates.
 *
 * * Once subscribers: these subscribers are removed after being notified once.
 *
 * * Timeout subscribers: these subscribers are removed after being notified once,
 * * but they may also timeout and be removed automatically after a certain delay
 * * if their subscribed message does not arrive.
 */
public final class ChordDispatcher {

    private final ConcurrentHashMap<ChordMessageKey, ChordObserverList> observerMap;
    private final ScheduledThreadPoolExecutor pool;

    private static final ChordDispatcher instance = new ChordDispatcher();

    public static ChordDispatcher get() {
        return instance;
    }

    private ChordDispatcher() {
        this.observerMap = new ConcurrentHashMap<>();
        this.pool = new ScheduledThreadPoolExecutor(DISPATCHER_TASKS_POOL_SIZE);
    }

    ScheduledThreadPoolExecutor getPool() {
        return pool;
    }

    public void addObserver(ChordObserver observer) {
        ChordMessageKey key = observer.getSubscribedKey();
        observerMap.computeIfAbsent(key, ChordObserverList::new).add(observer);
    }

    public boolean removeObserver(ChordObserver observer) {
        ChordMessageKey key = observer.getSubscribedKey();
        ChordObserverList list = observerMap.get(key);
        return list == null || list.remove(observer);
    }

    public void dispatch(ChordMessage message) {
        ChordMessageKey key = message.getKey();
        observerMap.computeIfAbsent(key, ChordObserverList::new).dispatch(message);
    }
}
