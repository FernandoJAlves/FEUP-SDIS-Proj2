package dbs.network;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import dbs.chord.NodeInfo;
import dbs.chord.messages.ChordMessage;

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

        accepterThread = new Thread(this);
        accepterThread.start();
        new Thread(new Dump()).start();
    }

    // public static SocketManager create(ServerSocketFactory serverFactory,
    // SocketFactory socketFactory)
    // throws IOException {
    // return new SocketManager(serverFactory, socketFactory);
    // }

    // private SocketManager(ServerSocketFactory serverFactory, SocketFactory
    // socketFactory) throws IOException {
    // assert instance == null;

    // this.server = serverFactory.createServerSocket();
    // this.listeners = new ConcurrentHashMap<>();
    // this.factory = socketFactory;
    // instance = this;

    // accepterThread = new Thread(this);
    // accepterThread.start();
    // }

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
    public boolean sendMessage(NodeInfo remoteNode, ChordMessage message) {
        System.out.println("Sending message " + message + " to " + remoteNode);

        ChordListener listener = listeners.get(remoteNode.getChordId());
        if (listener == null) {
            System.out.println("No listener for " + remoteNode);
            listener = open(remoteNode);
            if (listener == null) {
                System.out.println("Could not open socket for remote node " + remoteNode);
                return false;
            } else {
                System.out.println("Opened socket for remote node " + remoteNode);
            }
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
        if (old != null && old != listener)
            old.close();
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

    private class Dump implements Runnable {

        @Override
        public void run() {
            while (true) {
                dumpServer();

                try {
                    Thread.sleep(10000, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
