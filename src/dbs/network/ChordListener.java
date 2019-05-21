package dbs.network;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;

/**
 * Implements handleMessage() for the Chord protocol. We can use an Observer pattern here
 * or anything else.
 */
public class ChordListener extends Listener {
    ChordListener(Socket socket) {
        super(socket);
    }

    ChordListener(InetSocketAddress socketAddress, SocketFactory factory)
        throws IOException {
        super(socketAddress, factory);
    }

    @Override
    void handleMessage(Serializable object) {}
}
