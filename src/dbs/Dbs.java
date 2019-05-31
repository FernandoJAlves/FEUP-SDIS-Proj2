package dbs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import dbs.chord.Chord;
import dbs.chord.ChordDispatcher;
import dbs.chord.ChordLogger;
import dbs.chord.Node;
import dbs.chord.NodeInfo;
import dbs.chord.RestoreReturn;
import dbs.chord.messages.protocol.BackupMessage;
import dbs.chord.messages.protocol.DeleteMessage;
import dbs.chord.messages.protocol.RestoreMessage;
import dbs.chord.messages.protocol.TransferMessage;
import dbs.chord.observers.protocols.BackupResponseObserver;
import dbs.chord.observers.protocols.DeleteResponseObserver;
import dbs.chord.observers.protocols.RestoreResponseObserver;
import dbs.filesystem.FileManager;
import dbs.filesystem.threads.ResultCode;
import dbs.network.SocketManager;

public class Dbs implements RemoteInterface {

    private static Dbs instance;

    private final ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(Chord.DBS_TASKS_POOL_SIZE);

    public static Dbs get() {
        return instance;
    }

    private static void setupRMI(String name) {
        try {
            instance = new Dbs();
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(instance, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(name, stub);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        sks.load(new FileInputStream("cert/server.private"), serverPass.toCharArray());
        // setup client keystore
        KeyStore cks = KeyStore.getInstance("JKS");
        cks.load(new FileInputStream("cert/client.public"), "public".toCharArray());
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

        setupRMI(args[2]);

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

        setupRMI(args[2]);

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

    /**
     * Launch a lookup request for each of the given chord ids.
     */
    private ArrayList<CompletableFuture<NodeInfo>> lookupAll(BigInteger[] ids) {
        ArrayList<CompletableFuture<NodeInfo>> futures = new ArrayList<>();

        for (int i = 0; i < ids.length; i++) {
            futures.add(Node.get().lookup(ids[i]));
        }

        return futures;
    }

    /**
     * Launch a lookup request for each of the offset base ids...
     */
    private ArrayList<CompletableFuture<NodeInfo>> lookupAll(BigInteger baseId, int R) {
        return lookupAll(Chord.offsets(baseId, R));
    }

    /**
     * Wait on one lookup future.
     */
    private NodeInfo waitLookup(CompletableFuture<NodeInfo> lookupFuture) {
        assert lookupFuture != null;
        try {
            NodeInfo remoteNode = lookupFuture.get();
            return remoteNode;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Wait on several lookup futures.
     */
    private NodeInfo[] waitAllLookups(ArrayList<CompletableFuture<NodeInfo>> lookupFutures) {
        assert lookupFutures != null;
        try {
            NodeInfo[] remoteNodes = new NodeInfo[lookupFutures.size()];
            for (int i = 0; i < lookupFutures.size(); i++) {
                remoteNodes[i] = lookupFutures.get(i).get();
            }
            return remoteNodes;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Wait on file lookup.
     */
    private byte[] waitFile(CompletableFuture<byte[]> fileFuture) {
        assert fileFuture != null;
        try {
            byte[] file = fileFuture.get();
            return file;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Wait on several result codes.
     */
    private ResultCode[] waitAllCodes(ArrayList<CompletableFuture<ResultCode>> codeFutures) {
        try {
            ResultCode[] codes = new ResultCode[codeFutures.size()];
            for (int i = 0; i < codeFutures.size(); i++) {
                codes[i] = codeFutures.get(i).get();
            }
            return codes;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String iR(BigInteger id, int i, int R) {
        return String.format("{%s %d/%d}", Chord.percentStr(id), i + 1, R);
    }

    @Override
    public void backup(String fileName, int R) {
        assert fileName != null && R > 0;

        BigInteger fileId = Chord.encodeSHA256((new File(fileName)).getName());
        ChordLogger.logBackup("Filename: " + fileName + " | file id: " + Chord.percentStr(fileId));

        // collect offsets and lookup futures.
        BigInteger[] offsetIds = Chord.offsets(fileId, R);
        ArrayList<CompletableFuture<NodeInfo>> lookupFutures = lookupAll(fileId, R);

        // prepare reader and launch it in a different thread.
        CompletableFuture<byte[]> fileFuture = FileManager.getInstance().launchBackupReader(fileName);

        byte[] file = waitFile(fileFuture);

        NodeInfo[] remoteNodes = waitAllLookups(lookupFutures);

        // add file to our repository.
        Node.get().addFile(fileId, R);

        ArrayList<CompletableFuture<ResultCode>> codeFutures = new ArrayList<>();

        for (int i = 0; i < R; i++) {
            NodeInfo remoteNode = remoteNodes[i];
            BigInteger offsetFileId = offsetIds[i];
            String ir = "instance " + iR(offsetFileId, i, R);

            // No backup
            if (remoteNode == null) {
                ChordLogger.logBackup(fileName, ir + " is null, skipped");
                codeFutures.add(CompletableFuture.completedFuture(null));
            }
            // Self backup
            else if (remoteNode.equals(Node.get().getSelf())) {
                ChordLogger.logBackup(fileName, ir + " stored in this node");
                codeFutures.add(CompletableFuture.completedFuture(ResultCode.OK));
                FileManager.getInstance().launchBackupWriter(offsetFileId, file);
            }
            // Remote backup
            else {
                ChordLogger.logBackup(fileName, ir + " stored in remote node, sending message..");

                CompletableFuture<ResultCode> codeFuture = new CompletableFuture<>();
                codeFutures.add(codeFuture);

                // create observer and message
                BackupResponseObserver observer = new BackupResponseObserver(offsetFileId, codeFuture);
                BackupMessage message = new BackupMessage(offsetFileId, file);

                // add observer, and only then send the message
                ChordDispatcher.get().addObserver(observer);
                SocketManager.get().sendMessage(remoteNode, message);
            }
        }

        ResultCode[] codes = waitAllCodes(codeFutures);

        for (int i = 0; i < codes.length; ++i) {
            // ...
        }
    }

    @Override
    public void restore(String fileName) {
        assert fileName != null;

        BigInteger fileId = Chord.encodeSHA256(fileName);
        ChordLogger.logRestore("Filename: " + fileName + " | file id: " + Chord.percentStr(fileId));

        Integer Rp = Node.get().getReplicationMap().get(fileId);

        if (Rp == null) {
            ChordLogger.logSevere("File id " + Chord.percentStr(fileId) + " not found in this node");
            return;
        }

        int R = Rp;

        // Iterate through the offsets, trying to restore the node.
        for (int i = 0; i < R; ++i) {
            BigInteger offsetFileId = Chord.offset(fileId, i, R);
            String ir = "run " + iR(offsetFileId, i, R);

            CompletableFuture<NodeInfo> future = Node.get().lookup(offsetFileId);
            NodeInfo responsible = waitLookup(future);

            // No resolve
            if (responsible == null) {
                ChordLogger.logRestore(fileName, ir + " failed to resolve");
                continue;
            }
            // Self resolve
            else if (responsible.equals(Node.get().getSelf())) {
                ChordLogger.logRestore(fileName, ir + " resolved to this node");
                FileManager.getInstance().restoreFromBackup(offsetFileId.toString());
                // copy backup/filename -> restore/filename
                break;
            }
            // Remote resolve
            else {
                ChordLogger.logRestore(fileName, ir + " resolved to remote " + responsible.shortStr());

                CompletableFuture<RestoreReturn> restoreFuture = new CompletableFuture<>();

                // create observer and message
                RestoreResponseObserver observer = new RestoreResponseObserver(offsetFileId, restoreFuture);
                RestoreMessage message = new RestoreMessage(offsetFileId);

                // add observer, and only then send the message
                ChordDispatcher.get().addObserver(observer);
                SocketManager.get().sendMessage(responsible, message);

                RestoreReturn restoreReturn;
                try {
                    restoreReturn = restoreFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

                FileManager.getInstance().launchRestoreWriter(fileId.toString(), restoreReturn.getFileContent());

                break;
            }
        }
    }

    @Override
    public void delete(String fileName) {
        assert fileName != null;

        BigInteger fileId = Chord.encodeSHA256(fileName);
        ChordLogger.logDelete("Filename: " + fileName + " | file id: " + Chord.percentStr(fileId));

        Integer Rp = Node.get().getReplicationMap().get(fileId);

        if (Rp == null) {
            ChordLogger.logSevere("File id " + Chord.percentStr(fileId) + " not found in this node");
            return;
        }

        int R = Rp;

        // collect offsets and lookup futures.
        BigInteger[] offsetIds = Chord.offsets(fileId, R);
        ArrayList<CompletableFuture<NodeInfo>> lookupFutures = lookupAll(fileId, R);

        // espera que todos os lookups retornem
        NodeInfo[] remoteNodes = new NodeInfo[R];
        try {
            for (int i = 0; i < R; i++) {
                remoteNodes[i] = lookupFutures.get(i).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }

        // Enviar mensagens delete
        ArrayList<CompletableFuture<ResultCode>> codeFutures = new ArrayList<>();
        ResultCode[] resultCodes = new ResultCode[R];

        for (int i = 0; i < R; i++) {
            NodeInfo remoteNode = remoteNodes[i];
            BigInteger offsetFileId = offsetIds[i];
            String ir = "delete " + iR(offsetFileId, i, R);

            // No Delete
            if (remoteNode == null) {
                ChordLogger.logDelete(ir + " is null, skipped");
                continue;
            }
            // Self Delete
            else if (remoteNode.equals(Node.get().getSelf())) {
                ChordLogger.logDelete(fileName, ir + " stored in this node");
                codeFutures.add(CompletableFuture.completedFuture(ResultCode.OK));
                FileManager.getInstance().launchEraser(offsetFileId);
            }
            // Remote Delete
            else{
                ChordLogger.logDelete(fileName, ir + " stored in remote node, sending message..");

                CompletableFuture<ResultCode> codeFuture = new CompletableFuture<>();
                codeFutures.add(codeFuture);
    
                // create observer and message
                DeleteResponseObserver observer = new DeleteResponseObserver(offsetFileId, codeFuture);
                DeleteMessage message = new DeleteMessage(offsetFileId);
    
                // add observer, and only then send the message
                ChordDispatcher.get().addObserver(observer);
                SocketManager.get().sendMessage(remoteNode, message);
            }
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

        Node.get().getReplicationMap().remove(fileId);
    }

    @Override
    public int reclaim(int maxSize){

        int currSize = 0;

        List <Path> result;
        try (Stream<Path> walk = Files.walk(Paths.get(FileManager.BACKUP_FOLDER))) {
          result = walk.filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException e) {
          e.printStackTrace();
          return -1;
        }
    
        // Get current size
        for (Path path : result) {
          File f = path.toFile();
          currSize += f.length();
        }

        while(currSize > maxSize){
            for (Path path : result) {
                File f = path.toFile();

                currSize -= f.length();

                System.out.println("CURRSIZE: " + currSize);
                f.delete();
            }
        }


        return currSize;
    }

    @Override
    public String state(){
        String out = new String();
        String lineBreak = "================\n";
        String tab = "\t";

        out += lineBreak;
        out += "State of Node: \n";
        out += tab + "ID: " + Node.get().getSelf().getChordId() + '\n';
        out += tab + "Address: " + Node.get().getSelf().getIp() + '\n';
        out += tab + "Port: " + Node.get().getSelf().getPort() + '\n';
        out += "Files Backed Up: \n";
        for (HashMap.Entry<BigInteger, Integer> entry : Node.get().getReplicationMap().entrySet()) {
            out += tab + "FileId: " + entry.getKey() + "  =>  RepDegree: " + entry.getValue() + '\n';
        }
        out += lineBreak;

        return out;
    }

    public void transfer(NodeInfo predecessorNode) {
        ArrayList<BigInteger> ids = FileManager.getInstance().getFilesToTransfer(predecessorNode);
        int numIds = ids.size();
        ChordLogger.logTransfer("Transferring " + numIds + " backups to " + predecessorNode.shortStr());

        for (BigInteger id : ids) {
            pool.submit(new Transferer(id, predecessorNode));
        }
    }

    private class Transferer implements Runnable {

        private final BigInteger fileId;
        private final NodeInfo predecessorNode;

        Transferer(BigInteger fileId, NodeInfo predecessorNode) {
            assert fileId != null && predecessorNode != null;
            this.fileId = fileId;
            this.predecessorNode = predecessorNode;
        }

        @Override
        public void run() {
            // cancelar se o predecessor mudou

            byte[] file = waitFile(FileManager.getInstance().launchRestoreReader(fileId));
            FileManager.getInstance().launchEraser(fileId);
            TransferMessage message = new TransferMessage(fileId, file);
            SocketManager.get().sendMessage(predecessorNode, message);

            String fileStr = Chord.percentStr(fileId), predStr = predecessorNode.shortStr();
            ChordLogger.logTransfer("Transferred file " + fileStr + " to " + predStr);
        }
    }
}
