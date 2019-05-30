package dbs;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import dbs.chord.Chord;
import dbs.chord.ChordDispatcher;
import dbs.chord.Node;
import dbs.chord.NodeInfo;
import dbs.chord.messages.protocol.BackupMessage;
import dbs.chord.observers.protocols.BackupResponseObserver;
import dbs.network.SocketManager;

public class Dbs implements RemoteInterface {

    public static void main(String[] args) throws IOException {
        if (args.length <= 1)
            usage();

        switch (args[0]) {
        case "join":
            join(args);
            break;
        case "create":
            create(args);
            break;
        case "print":
            print(args);
            break;
        default:
            usage();
        }
    }

    static void join(String[] args) throws IOException {
        if (args.length != 6)
            usage();

        InetAddress address = InetAddress.getByName(args[1]);
        int port = Integer.parseInt(args[2]);
        InetSocketAddress serverAddress = new InetSocketAddress(address, port);

        final ServerSocketFactory serverFactory = ServerSocketFactory.getDefault();
        final SocketFactory socketFactory = SocketFactory.getDefault();

        SocketManager.create(serverAddress, serverFactory, socketFactory);
        Node.create(serverAddress);
        BigInteger remoteId = new BigInteger(args[3]);
        InetAddress remoteAddress = InetAddress.getByName(args[4]);
        int remotePort = Integer.parseInt(args[5]);
        InetSocketAddress remoteServerAddress = new InetSocketAddress(remoteAddress, remotePort);
        Node.get().join(new NodeInfo(remoteId, remoteServerAddress));
    }

    static void create(String[] args) throws IOException {
        if (args.length != 3)
            usage();

        final ServerSocketFactory serverFactory = ServerSocketFactory.getDefault();
        final SocketFactory socketFactory = SocketFactory.getDefault();

        InetAddress address = InetAddress.getByName(args[1]);
        int port = Integer.parseInt(args[2]);
        InetSocketAddress serverAddress = new InetSocketAddress(address, port);

        SocketManager.create(serverAddress, serverFactory, socketFactory);
        Node.create(serverAddress);
        Node.get().join();
    }

    static void print(String[] args) throws IOException {
        if (args.length < 3)
            usage();

        InetAddress address = InetAddress.getByName(args[1]);

        for (int i = 2; i < args.length; ++i) {
            int port = Integer.parseInt(args[i]);
            BigInteger id = Chord.consistentHash(address, port);
            // String percentage = Chord.percentStr(id);
            System.out.println(id);
        }

        System.exit(0);
    }

    static void usage() {
        System.err.println("Usage:");
        System.err.println("Dbs join ADDRESS PORT REMOTE_ID REMOTE_ADDRESS REMOTE_PORT");
        System.err.println("Dbs create ADDRESS PORT");
        System.err.println("Dbs print ADDRESS PORT1 PORT2 PORT3 PORT4 ...");
        System.exit(0);
    }

    private byte[] getFile(String filepath) {
        return new byte[10];
    }

    private ArrayList<CompletableFuture<NodeInfo>> lookupAll(BigInteger baseId, int R) {
        BigInteger[] ids = Chord.offsets(baseId, R);

        @SuppressWarnings("unchecked")
        ArrayList<CompletableFuture<NodeInfo>> futures = new ArrayList<>();

        for (int i = 0; i < R; ++i) {
            futures.add(Node.get().lookup(ids[i]));
        }

        return futures;
    }

    @Override
    public void backup(String filepath, int R) {
        assert filepath != null && R > 0;

        BigInteger fileId = Chord.encodeSHA256(filepath);

        ArrayList<CompletableFuture<NodeInfo>> lookupFutures = lookupAll(fileId, R);
        NodeInfo[] remoteNodes = new NodeInfo[R];

        byte[] file = getFile(filepath); // bloqueia e pode dar throw.
        assert file != null;

        try {
            for (int i = 0; i < R; ++i) {
                remoteNodes[i] = lookupFutures.get(i).get();
                // pode ser null
            }
        } catch (InterruptedException | ExecutionException e) {
            // shouldn't happen, except perhaps with Ctrl+C and such interactions.
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }

        // mudar para array de codes:
        CompletableFuture<Integer> codeFuture = new CompletableFuture<>();
        int resultCode;

        // array de observers:
        BackupResponseObserver observer = new BackupResponseObserver(fileId, codeFuture);

        // 1 mensagem:
        BackupMessage message = new BackupMessage(fileId, file);

        // add send loop:
        {
            ChordDispatcher.get().addObserver(observer);
            SocketManager.get().sendMessage(__, message);
        }

        // ...
        try {
            resultCode = codeFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            // shouldn't happen, except perhaps with Ctrl+C and such interactions.
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }

    }

    @Override
    public void restore(String filepath) {

    }

    @Override
    public void delete(String pathname) {

    }

}
