package dbs.network;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import dbs.chord.NodeServerInfo;

public class SocketManager implements Runnable {

    private final ConcurrentHashMap<BigInteger, ChordListener> listeners;
    private final ServerSocket server;
    private final SocketFactory factory;

    private final Thread accepterThread;

    private static SocketManager instance;

    public static SocketManager get() {
        return instance;
    }

    public static SocketManager create(InetSocketAddress serverAddress, ServerSocketFactory serverFactory,
            SocketFactory socketFactory) throws IOException {
        return new SocketManager(serverAddress, serverFactory, socketFactory);
    }

    SocketManager(InetSocketAddress serverAddress, ServerSocketFactory serverFactory, SocketFactory socketFactory)
            throws IOException {
        assert instance == null;
        int port = serverAddress.getPort();
        InetAddress address = serverAddress.getAddress();

        this.server = serverFactory.createServerSocket(port, 15, address);
        this.listeners = new ConcurrentHashMap<>();
        this.factory = socketFactory;
        instance = this;

        accepterThread = new Thread(this);
        accepterThread.start();
    }

    @Override
    public void run() {
        while (true) {
            if (server.isClosed())
                break;
            try {
                Socket socket = server.accept();
                if (socket == null)
                    continue;
                new ChordListener(socket);
            } catch (IOException e) {
                e.printStackTrace();
                if (server.isClosed())
                    break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Attempts to send a serializable message to the socket of this peer.
     * Otherwise, attempts to create a new socket connected to the given node's
     * server address.
     */
    public boolean sendMessage(NodeServerInfo remoteNode, Serializable message) {
        ChordListener listener = listeners.get(remoteNode.getChordId());
        if (listener == null) {
            listener = open(remoteNode);
            if (listener == null)
                return false;
        }
        return listener.sendMessage(message);
    }

    /**
     * Called by a ChordListener to register itself in the listeners map after a
     * connection, once a remote Node has properly identified itself with a first
     * message.
     */
    void setListener(ChordListener listener) {
        ChordListener old = listeners.put(listener.getRemoteNode().getChordId(), listener);
        if (old != null)
            old.close();
    }

    void removeListener(ChordListener listener) {
        listeners.remove(listener.getRemoteNode().getChordId(), listener);
    }

    private synchronized ChordListener open(NodeServerInfo remoteNode) {
        try {
            InetAddress address = remoteNode.getIp();
            int port = remoteNode.getPort();
            Socket socket = factory.createSocket(address, port);
            ChordListener listener = new ChordListener(socket, remoteNode);
            return listener;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
