package dbs.chord;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;

import dbs.chord.messages.ChordMessage;
import dbs.network.Listener;

/**
 * Implements handleMessage() for the Chord protocol. We can use an Observer
 * pattern here or anything else.
 */
public final class ChordListener extends Listener {
    public ChordListener(Socket socket) {
        super(socket);
    }

    @Override
    protected void handleMessage(Serializable object) {
        if (!(object instanceof ChordMessage)) {
            System.err.println("Received message not of class ChordMessage.");
            return;
        }

        ChordMessage message = (ChordMessage) object;
        InetSocketAddress localAddress = getLocalAddress();
        ChordDispatcher.get().dispatch(message, localAddress);
    }
}
