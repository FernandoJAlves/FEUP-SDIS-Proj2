package dbs.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;

/**
 * A Listener is launched on an accepted socket connection OR on a created
 * socket connection. The concrete Listener implementation may vary as
 * handleMessage() is abstract.
 *
 * When a Listener is created, it will try (after opening the socket stream if
 * not already open) to open the input and output streams. If successful, it is
 * now ready for both input and output operations. It may be the case that
 * another Listener has already been registered in the SocketManager, for the
 * same socket address; in that case, the new Listener will replace the old one,
 * and inform the old one it should promptly close the connection (todo). All
 * requests for Listeners for the given socket address will then point to the
 * new Listener.
 */
public abstract class Listener implements Runnable, Closeable {

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private InetSocketAddress socketAddress;
    private boolean closed = false;

    private Thread thread;

    protected Listener(Socket socket) {
        this.socketAddress = (InetSocketAddress) socket.getLocalSocketAddress();
        this.socket = socket;
        openStreams();
    }

    protected Listener(InetSocketAddress socketAddress, SocketFactory factory) {
        this.socketAddress = null;
        try {
            InetAddress address = socketAddress.getAddress();
            int port = socketAddress.getPort();
            this.socket = factory.createSocket(address, port);
            this.socketAddress = (InetSocketAddress) this.socket.getLocalSocketAddress();
            openStreams();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            if (isClosed())
                break;
            try {
                Object object = input.readObject();
                if (object != null)
                    handleMessage((Serializable) object);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected InetSocketAddress getLocalAddress() {
        return socketAddress;
    }

    synchronized boolean sendMessage(Serializable object) {
        try {
            output.writeObject(object);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized void close() {
        if (isClosed())
            return;

        closed = true;

        try {
            if (input != null)
                input.close();
            if (output != null)
                output.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract void handleMessage(Serializable object);

    private final boolean isClosed() {
        return socket.isClosed() || closed;
    }

    private void openStreams() {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            this.input = new ObjectInputStream(new BufferedInputStream(in));
            this.output = new ObjectOutputStream(new BufferedOutputStream(out));
            SocketManager.get().setListener(this);
            thread = new Thread(this);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
            this.close();
        }
    }
}
