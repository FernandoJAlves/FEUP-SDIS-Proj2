package dbs.network;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import dbs.chord.ChordLogger;
import dbs.chord.NodeInfo;
import dbs.chord.messages.ChordMessage;

public class SocketManager {

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

    private SocketManager(InetSocketAddress serverAddress, ServerSocketFactory serverFactory,
            SocketFactory socketFactory) throws IOException {
        assert instance == null;
        int port = serverAddress.getPort();
        InetAddress address = serverAddress.getAddress();

        this.server = serverFactory.createServerSocket(port, 15, address);
        this.listeners = new ConcurrentHashMap<>();
        this.factory = socketFactory;
        instance = this;

        dumpServer();

        accepterThread = new Thread(new Accepter());
        accepterThread.start();
    }

    /**
     * Attempts to send a serializable message to the socket of this peer.
     * Otherwise, attempts to create a new socket connected to the given node's
     * server address.
     */
    public boolean sendMessage(NodeInfo remoteNode, ChordMessage message) {
        ChordListener listener = listeners.get(remoteNode.getChordId());
        if (listener == null) {
            listener = open(remoteNode);
            if (listener == null) {
                ChordLogger.logSocket("Could not open socket for remote node " + remoteNode.shortStr());
                return false;
            } else {
                ChordLogger.logSocket("Opened socket for remote node " + remoteNode.shortStr());
            }
        }
        boolean success = listener.sendMessage(message);
        if (!success)
            removeListener(listener);
        return success;
    }

    /**
     * Called by a ChordListener to register itself in the listeners map after a
     * connection, once a remote Node has properly identified itself with a first
     * message.
     */
    void setListener(ChordListener listener) {
        ChordListener old = listeners.put(listener.getRemoteNode().getChordId(), listener);
        if (old != null && old != listener)
            old.finish();
    }

    void removeListener(ChordListener listener) {
        listeners.remove(listener.getRemoteNode().getChordId(), listener);
    }

    public boolean tryOpen(NodeInfo remoteNode) {
        if (listeners.containsKey(remoteNode.getChordId()))
            return true;

        return open(remoteNode) != null;
    }

    private synchronized ChordListener open(NodeInfo remoteNode) {
        InetAddress address = remoteNode.getIp();
        int port = remoteNode.getPort();

        try {
            Socket socket = factory.createSocket(address, port);
            ChordListener listener = new ChordListener(socket, remoteNode);
            return listener;
        } catch (ConnectException e) {
            ChordLogger.logSocket("Failed to connect to socket " + address + ":" + port);
        } catch (IOException e) {
            ChordLogger.socketError(e);
        }

        return null;
    }

    public void dumpServer() {
        System.out.println("SocketManager's server bound to");
        System.out.println("  address: " + server.getInetAddress());
        System.out.println("  port:    " + server.getLocalPort());
        for (BigInteger chordId : listeners.keySet()) {
            ChordListener listener = listeners.get(chordId);
            System.out.println("listener on node " + chordId);
            System.out.println("connected:" + listener.isConnected() + ", " + listener.getRemoteNode());
        }
    }

    private class Accepter implements Runnable {

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
    }
}
