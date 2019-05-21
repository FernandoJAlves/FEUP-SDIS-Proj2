package dbs.chord.observers;

import java.math.BigInteger;
import java.net.InetSocketAddress;

import dbs.chord.messages.ChordMessage;

public final class LookupObserver extends ChordObserver {

    public LookupObserver(BigInteger chordid) {
        super("RESPONSIBLE", chordid);
    }

    @Override
    public boolean notify(ChordMessage message, InetSocketAddress sourceAddress) {
        return true;
    }
}
