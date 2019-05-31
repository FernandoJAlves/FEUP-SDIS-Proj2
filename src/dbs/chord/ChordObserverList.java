package dbs.chord;

import java.util.Iterator;
import java.util.LinkedList;
import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.ChordMessageKey;
import dbs.chord.observers.ChordObserver;
import dbs.chord.observers.OnceObserver;
import dbs.chord.observers.TimeoutObserver;

public final class ChordObserverList {

    private final ChordMessageKey key;
    private final LinkedList<ChordObserver> observers;

    ChordObserverList(ChordMessageKey key) {
        this.key = key;
        this.observers = new LinkedList<>();
    }

    synchronized void add(ChordObserver observer) {
        this.observers.add(observer);

        if (observer instanceof TimeoutObserver) {
            TimeoutObserver timeoutObserver = (TimeoutObserver) observer;
            int delay = timeoutObserver.getDelay();
            TimeoutAction task = new TimeoutAction(timeoutObserver);
            ChordDispatcher.get().getPool().schedule(task, delay, TimeoutObserver.unit);
        }
    }

    synchronized boolean remove(ChordObserver observer) {
        return this.observers.remove(observer);
    }

    synchronized void dispatch(ChordMessage message) {
        Iterator<ChordObserver> iterator = observers.iterator();

        if (!iterator.hasNext()) {
            ChordLogger.internal("Unexpected: no observers waiting on messages " + key);
            return;
        }

        while (iterator.hasNext()) {
            ChordObserver observer = iterator.next();

            if (observer instanceof OnceObserver || observer instanceof TimeoutObserver)
                iterator.remove();

            InvokeNotify task = new InvokeNotify(observer, message);
            ChordDispatcher.get().getPool().submit(task);
        }
    }

    ChordMessageKey getSubscribedKey() {
        return key;
    }

    void dump() {
        System.out.println("Listeners on " + key);
        for (ChordObserver observer : observers) {
            System.out.println(observer);
        }
    }

    class InvokeNotify implements Runnable {

        private final ChordObserver observer;
        private final ChordMessage message;

        private InvokeNotify(ChordObserver observer, ChordMessage message) {
            this.observer = observer;
            this.message = message;
        }

        @Override
        public void run() {
            observer.notify(message);
        }
    }

    class TimeoutAction implements Runnable {

        private final TimeoutObserver observer;

        private TimeoutAction(TimeoutObserver observer) {
            this.observer = observer;
        }

        @Override
        public void run() {
            if (remove(observer))
                observer.timeout();
        }
    }
}
