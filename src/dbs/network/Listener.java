package dbs.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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

public abstract class Listener implements Runnable {
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private InetSocketAddress socketAddress;

    private Thread thread;

    Listener(Socket socket) {
        this.socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();

        try {
            this.socket = socket;
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            this.input = new ObjectInputStream(new BufferedInputStream(in));
            this.output = new ObjectOutputStream(new BufferedOutputStream(out));
            SocketManager.get().setListener(this);
            startListening();
        } catch (IOException e) {
            e.printStackTrace();
            this.close();
        }
    }

    Listener(InetSocketAddress socketAddress, SocketFactory factory) throws IOException {
        InetAddress address = socketAddress.getAddress();
        int port = socketAddress.getPort();
        this.socketAddress = socketAddress;

        try {
            this.socket = factory.createSocket(address, port);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            this.input = new ObjectInputStream(new BufferedInputStream(in));
            this.output = new ObjectOutputStream(new BufferedOutputStream(out));
            SocketManager.get().setListener(this);
            startListening();
        } catch (IOException e) {
            e.printStackTrace();
            this.close();
        }
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public synchronized boolean sendMessage(Serializable object) {
        try {
            output.writeObject(object);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run() {
        while (true) {
            if (socket.isClosed()) break;
            try {
                Object object = input.readObject();
                if (object == null) continue;
                handleMessage((Serializable) object);
            } catch (IOException e) {
                e.printStackTrace();
                if (socket.isClosed()) break;
            } catch (Exception e) {
                // Discard any other exception
                e.printStackTrace();
            }
        }
    }

    protected synchronized void close() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Deu peido a fechar as streams/sockets...");
            e.printStackTrace();
        }
    }

    protected abstract void handleMessage(Serializable object);

    private void startListening() {
        thread = new Thread(this);
        thread.start();
    }
}
