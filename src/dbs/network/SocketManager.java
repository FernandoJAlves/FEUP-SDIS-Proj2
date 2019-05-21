package dbs.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

public class SocketManager implements Runnable {
    private final ConcurrentHashMap<SocketAddress, Listener> listeners;
    private final ServerSocket server;
    private final SocketFactory factory;

    private Thread accepterThread;

    private static SocketManager instance;

    public static SocketManager get() {
        assert instance != null;
        return instance;
    }

    public static SocketManager create(InetSocketAddress socketAddress,
                                       ServerSocketFactory serverFactory,
                                       SocketFactory socketFactory) throws IOException {
        assert instance == null;
        instance = new SocketManager(socketAddress, serverFactory, socketFactory);
        return instance;
    }

    SocketManager(InetSocketAddress socketAddress, ServerSocketFactory serverFactory,
                  SocketFactory socketFactory) throws IOException {
        int port = socketAddress.getPort();
        InetAddress address = socketAddress.getAddress();

        this.server = serverFactory.createServerSocket(port, 15, address);
        this.listeners = new ConcurrentHashMap<>();
        this.factory = socketFactory;

        startListening();
    }

    void setListener(Listener listener) {
        Listener old = listeners.put(listener.getSocketAddress(), listener);
        if (old != null) old.close();
    }

    void removeListener(Listener listener) {
        listeners.remove(listener.getSocketAddress());
        listener.close();
    }

    Listener get(InetSocketAddress socketAddress) {
        Listener listener = listeners.get(socketAddress);
        if (listener == null) {
            try {
                listener = new Listener(socketAddress, factory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            if (socket.isClosed()) break;
            try {
                Socket socket = server.accept();
                if (socket == null) continue;

            } catch (IOException e) {
                e.printStackTrace();
                if (server.isClosed()) break;
            } catch (Exception e) {
                // Discard any other exception
                e.printStackTrace();
            }
        }
    }

    private void startListening() {
        accepterThread = new Thread(this);
        accepterThread.start();
    }
}
