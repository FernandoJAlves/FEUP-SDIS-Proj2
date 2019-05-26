package dbs.network;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketException;

import dbs.chord.ChordDispatcher;
import dbs.chord.Node;
import dbs.chord.NodeInfo;
import dbs.chord.messages.ChordMessage;

/**
 * A ChordListener is launched on an accepted socket connection OR on a created
 * socket connection. The concrete ChordListener implementation may vary as
 * handleMessage() is abstract.
 *
 * When a ChordListener is created, it will try (after opening the socket stream
 * if not already open) to open the input and output streams. If successful, it
 * is now ready for both input and output operations. It may be the case that
 * another ChordListener has already been registered in the SocketManager, for
 * the same socket address; in that case, the new ChordListener will replace the
 * old one, and inform the old one it should promptly close the connection
 * (todo). All requests for Listeners for the given socket address will then
 * point to the new ChordListener.
 */
public class ChordListener implements Runnable {

    private final Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;

    private boolean closed = false, connected = false;
    private NodeInfo remoteNode;

    private Thread thread;

    /**
     * Open streams and setup communications on an already opened and connected
     * Socket, which has been opened by our SocketManager for the given remoteNode.
     */
    ChordListener(Socket socket, NodeInfo remoteNode) {
        this.socket = socket;
        this.input = null;
        this.output = null;

        try {
            this.output = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            this.output.flush();
            this.input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            close();
            return;
        }

        this.remoteNode = remoteNode;
        this.connected = true;
        SocketManager.get().setListener(this);
        this.thread = new Thread(this);
        this.thread.start();
    }

    /**
     * Open streams and setup communications on an already opened and connected
     * Socket, which has been accepted by our SocketManager and has not yet been
     * identified.
     */
    ChordListener(Socket socket) {
        this.socket = socket;
        this.input = null;
        this.output = null;

        try {
            this.output = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            this.output.flush();
            this.input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            close();
            return;
        }

        this.thread = new Thread(this);
        this.thread.start();
    }

    boolean isConnected() {
        return true;
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
            } catch (IOException | ClassNotFoundException e) {
                close();
                return;
            }
        }
    }

    public synchronized void close() {
        if (remoteNode != null && connected) {
            System.out.println("Socket to " + remoteNode + " closed().");
            SocketManager.get().removeListener(this);
        }

        if (isClosed())
            return;

        closed = true;

        try {
            if (input != null && !socket.isInputShutdown())
                input.close();
            else if (output != null && !socket.isOutputShutdown())
                output.close();
            else if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    NodeInfo getRemoteNode() {
        return remoteNode;
    }

    synchronized boolean sendMessage(ChordMessage message) {
        assert remoteNode != null && output != null;

        try {
            output.writeObject(message);
            output.flush();
            return true;
        } catch (EOFException | SocketException e) {
            e.printStackTrace();
            close();
            return false;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void handleMessage(Serializable object) {
        if (!(object instanceof ChordMessage)) {
            System.err.println("Received message not of class ChordMessage.");
            return;
        }

        ChordMessage message = (ChordMessage) object;

        if (message.getSender().equals(Node.get().getSelf())) {
            System.err.println("MAJOR ERROR: Received message from myself: " + message);
            return;
        } else if (remoteNode != null && !message.getSender().equals(remoteNode)) {
            System.err.println("Received message from " + message.getSender().shortStr()
                    + ", but socket is connected to " + remoteNode.shortStr());
            return;
        }

        if (remoteNode == null) {
            remoteNode = message.getSender();
            connected = true;
            SocketManager.get().setListener(this);
        }

        System.out.println("\u001B[35mIN " + message + " from " + remoteNode.shortStr() + "\u001B[0m");

        ChordDispatcher.get().dispatch(message);
    }

    /**
     * @return true if this listener's socket has already been closed.
     */
    private final boolean isClosed() {
        return socket.isClosed() || closed;
    }
}
