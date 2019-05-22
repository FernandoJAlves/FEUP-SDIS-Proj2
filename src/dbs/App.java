package dbs;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import dbs.chord.Node;
import dbs.chord.NodeInfo;
import dbs.network.SocketManager;

public class App {
    public static void main(String[] args) throws IOException {
        ServerSocketFactory serverFactory = ServerSocketFactory.getDefault();
        SocketFactory socketFactory = SocketFactory.getDefault();

        if (args.length < 2)
            usage();

        String action = args[0];

        if (!action.equals("join") && !action.equals("create"))
            usage();

        InetAddress address = InetAddress.getByName(args[1]);
        int port = Integer.parseInt(args[2]);
        InetSocketAddress serverAddress = new InetSocketAddress(address, port);

        SocketManager.create(serverAddress, serverFactory, socketFactory);
        Node.create(serverAddress);

        switch (action) {
        case "join":
            BigInteger remoteId = new BigInteger(args[3]);
            InetAddress remoteAddress = InetAddress.getByName(args[4]);
            int remotePort = Integer.parseInt(args[5]);
            InetSocketAddress remoteServerAddress = new InetSocketAddress(remoteAddress, remotePort);
            Node.get().join(new NodeInfo(remoteId, remoteServerAddress));
            break;
        case "create":
            Node.get().join();
            break;
        }
    }

    static void usage() {
        System.err.println("Usage:");
        System.err.println("App join ADDRESS PORT REMOTE_ID REMOTE_ADDRESS REMOTE_PORT");
        System.err.println("App create ADDRESS PORT");
        System.exit(0);
    }
}
