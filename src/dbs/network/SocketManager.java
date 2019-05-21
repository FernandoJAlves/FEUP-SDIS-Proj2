package dbs.network;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

public class SocketManager implements Runnable {

    private final ConcurrentHashMap<InetSocketAddress, Listener> listeners;
    private final ServerSocket server;
    private final SocketFactory factory;

    private final Thread accepterThread;

    private static SocketManager instance;

    public static SocketManager get() {
        return instance;
    }

    public static SocketManager create(InetSocketAddress socketAddress, ServerSocketFactory serverFactory,
            SocketFactory socketFactory) throws IOException {
        return new SocketManager(socketAddress, serverFactory, socketFactory);
    }

    SocketManager(InetSocketAddress socketAddress, ServerSocketFactory serverFactory, SocketFactory socketFactory)
            throws IOException {
        assert instance == null;
        int port = socketAddress.getPort();
        InetAddress address = socketAddress.getAddress();

        this.server = serverFactory.createServerSocket(port, 15, address);
        this.listeners = new ConcurrentHashMap<>();
        this.factory = socketFactory;
        instance = this;

        accepterThread = new Thread(this);
        accepterThread.start();
    }

    void setListener(Listener listener) {
        Listener old = listeners.put(listener.getSocketAddress(), listener);
        if (old != null)
            old.close();
    }

    void removeListener(Listener listener) {
        listeners.remove(listener.getSocketAddress());
        listener.close();
    }

    Listener getListener(InetSocketAddress socketAddress) {
        Listener listener = listeners.get(socketAddress);
        if (listener != null)
            return listener;

        try {
            return new ChordListener(socketAddress, factory);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean sendMessage(InetSocketAddress socketAddress, Serializable message) {
        Listener listener = getListener(socketAddress);
        if (listener == null)
            return false;
        return listener.sendMessage(message);
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
}
