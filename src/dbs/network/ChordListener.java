package dbs.network;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;

import dbs.chord.ChordDispatcher;
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

    private boolean closed = false;
    private NodeInfo remoteNode;

    private Thread thread;

    ChordListener(Socket socket, NodeInfo remoteNode) {
        this.remoteNode = remoteNode;
        this.socket = socket;
        this.input = null;
        this.output = null;

        try {
            OutputStream out = socket.getOutputStream();
            this.output = new ObjectOutputStream(new BufferedOutputStream(out));
            this.output.flush();
            System.out.println("A");
            InputStream in = socket.getInputStream();
            this.input = new ObjectInputStream(in);
            System.out.println("B");
            SocketManager.get().setListener(this);
            thread = new Thread(this);
            thread.start();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            close();
        }
    }

    ChordListener(Socket socket) {
        this.socket = socket;
        this.input = null;
        this.output = null;

        try {
            OutputStream out = socket.getOutputStream();
            this.output = new ObjectOutputStream(new BufferedOutputStream(out));
            this.output.flush();
            System.out.println("A2");
            InputStream in = socket.getInputStream();
            this.input = new ObjectInputStream(in);
            System.out.println("B2");
            thread = new Thread(this);
            thread.start();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            close();
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
            } catch (EOFException e) {
                System.out.println("Socket to " + remoteNode + " closed.");
                this.close();
                break;
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println(e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    public synchronized void close() {
        if (isClosed())
            return;

        closed = true;

        if (socket != null && remoteNode != null)
            SocketManager.get().removeListener(this);

        try {
            if (input != null)
                input.close();
            else if (output != null)
                output.close();
            else if (socket != null)
                socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    NodeInfo getRemoteNode() {
        return remoteNode;
    }

    synchronized boolean sendMessage(Serializable object) {
        assert remoteNode != null && output != null;

        try {
            output.writeObject(object);
            output.flush();
            return true;
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

        if (remoteNode == null) {
            remoteNode = message.getSender();
            SocketManager.get().setListener(this);
        }

        ChordDispatcher.get().dispatch(message);
    }

    private final boolean isClosed() {
        return socket.isClosed() || closed;
    }
}
