package dbs;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import dbs.chord.Chord;
import dbs.chord.ChordDispatcher;
import dbs.chord.Node;
import dbs.chord.NodeInfo;
import dbs.chord.messages.protocol.BackupMessage;
import dbs.chord.observers.protocols.BackupResponseObserver;
import dbs.filesystem.Configuration;
import dbs.filesystem.FileManager;
import dbs.filesystem.threads.Reader;
import dbs.filesystem.threads.ResultCode;
import dbs.network.SocketManager;

public class Dbs implements RemoteInterface {

    private static SecureRandom secureRandom;
    private static SSLContext serverContext;
    private static SSLContext clientContext;
    private static String clientPass = "clientpw";
    private static String serverPass = "serverpw";

    public static void main(String[] args) throws Exception {

        secureRandom = new SecureRandom();
        secureRandom.nextInt();
        setupClientContext();
        setupServerContext();

        if (args.length <= 1)
            usage();
 
        // rmi
        try {
            Dbs dbs = new Dbs();
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(dbs, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(args[2], stub);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
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

    private static void setupClientContext() throws Exception {
        // setup server keystore
        KeyStore sks = KeyStore.getInstance("JKS");
        sks.load(new FileInputStream("cert/server.public"), "public".toCharArray());
        // setup client keystore
        KeyStore cks = KeyStore.getInstance("JKS");
        cks.load(new FileInputStream("cert/client.private"),
                       clientPass.toCharArray());
        // setup SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(sks);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(cks, clientPass.toCharArray());
        clientContext = SSLContext.getInstance("TLS");
        clientContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), secureRandom);
    }

    private static void setupServerContext() throws Exception {
        // setup server keystore
        KeyStore sks = KeyStore.getInstance("JKS");
        sks.load(new FileInputStream("cert/server.private"), "public".toCharArray());
        // setup client keystore
        KeyStore cks = KeyStore.getInstance("JKS");
        cks.load(new FileInputStream("cert/client.public"), serverPass.toCharArray());
        // setup SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(cks);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(sks, serverPass.toCharArray());
        serverContext = SSLContext.getInstance("TLS");
        serverContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), secureRandom);
    }



    static void join(String[] args) throws Exception {
        if (args.length != 6)
            usage();

        InetAddress address = InetAddress.getByName(args[1]);
        int port = Integer.parseInt(args[2]);
        InetSocketAddress serverAddress = new InetSocketAddress(address, port);

        final SSLSocketFactory socketFactory = clientContext.getSocketFactory();
        final SSLServerSocketFactory serverFactory = serverContext.getServerSocketFactory();

        SocketManager.create(serverAddress, serverFactory, socketFactory);
        Node.create(serverAddress);
        BigInteger remoteId = new BigInteger(args[3]);
        InetAddress remoteAddress = InetAddress.getByName(args[4]);
        int remotePort = Integer.parseInt(args[5]);
        InetSocketAddress remoteServerAddress = new InetSocketAddress(remoteAddress, remotePort);
        Node.get().join(new NodeInfo(remoteId, remoteServerAddress));
    }

    static void create(String[] args) throws Exception {
        
        if (args.length != 3)
            usage();

        final SSLSocketFactory socketFactory = clientContext.getSocketFactory();
        final SSLServerSocketFactory serverFactory = serverContext.getServerSocketFactory();

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

        CompletableFuture<byte[]> fileFuture = new CompletableFuture<>();
        Reader reader = null;
        try {
            reader = new Reader(filepath, fileFuture, Configuration.Operation.BACKUP);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileManager.getInstance().getThreadpool().submit(reader);

        // espera que o ficheiro esteja lido
        byte[] file = new byte[0]; // bloqueia e pode dar throw.
        try {
            file = fileFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assert file != null;

        // espera que todos os lookups retornem
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

        // mudado para array de codes:
        // CompletableFuture<Integer> codeFuture = new CompletableFuture<>();
        // int resultCode;

        ArrayList<CompletableFuture<ResultCode>> codeFutures = new ArrayList<>();
        ResultCode[] resultCodes = new ResultCode[R];

        // array de observers:
        BackupResponseObserver[] observerArray = new BackupResponseObserver[R];
        for (int i = 0; i < R; i++) {
            CompletableFuture<ResultCode> future = new CompletableFuture<>();
            codeFutures.add(future);
            observerArray[i] = new BackupResponseObserver(fileId, codeFutures.get(i));
        }

        // 1 mensagem:
        BackupMessage message = new BackupMessage(fileId, file);

        // send loop:
        for (int i = 0; i < R; i++) {
            ChordDispatcher.get().addObserver(observerArray[i]);
            //SocketManager.get().sendMessage(remoteNodes[i], message);
        }

        // get all result codes
        for (int i = 0; i < R; i++) {
            try {
                resultCodes[i] = codeFutures.get(i).get();
            } catch (InterruptedException | ExecutionException e) {
                // shouldn't happen, except perhaps with Ctrl+C and such interactions.
                System.err.println(e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        // replace replicationDeg with number sent (not the number of oks, otherwise it
        // would mess up future lookups)
        Node.get().addFile(fileId, R);
    }

    @Override
    public void restore(String filepath) {

    }

    @Override
    public void delete(String pathname) {
        HashMap<BigInteger, Integer> replicationMap = Node.get().getReplicationMap();
        // TODO: call function to retrieve offsets of nodes
    }
}
