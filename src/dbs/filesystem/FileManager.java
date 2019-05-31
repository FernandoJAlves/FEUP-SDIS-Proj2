package dbs.filesystem;

import dbs.chord.Chord;
import dbs.chord.Node;
import dbs.chord.NodeInfo;
import dbs.filesystem.messages.DeleteRequest;
import dbs.filesystem.messages.ReadRequest;
import dbs.filesystem.messages.Request;
import dbs.filesystem.messages.WriteRequest;
import dbs.filesystem.threads.Reader;

import java.io.File;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dbs.filesystem.Configuration.Operation;
import dbs.filesystem.threads.Writer;

/**
 * The FileManager is used to manage access to the filesystem, providing read, write
 * and delete operations using the Java NIO API and a thread pool
 */
public class FileManager implements Runnable {

  private static ThreadPoolExecutor threadpool = (ThreadPoolExecutor) Executors
      .newFixedThreadPool(Configuration.POOL_SIZE);
  public static String BACKUP_FOLDER;
  public static String RESTORE_FOLDER;

  private static FileManager instance;
  private final LinkedBlockingDeque<Request> queue;

  public FileManager() {
    this.queue = new LinkedBlockingDeque<>();
    this.createFilesystem();
  }

  public void createFilesystem() {
    String peerId = Node.get().getSelf().getChordId().toString();

    BACKUP_FOLDER = Configuration.PEER_FOLDER + "/" + peerId + "/backup/";
    RESTORE_FOLDER = Configuration.PEER_FOLDER + "/" + peerId + "/restore/";

    File backupDir = new File(BACKUP_FOLDER);
    if (!backupDir.exists())
      backupDir.mkdirs();
    File restoreDir = new File(RESTORE_FOLDER);
    if (!restoreDir.exists())
      restoreDir.mkdirs();
  }

  public static FileManager getInstance() {
    if (instance == null) {
      instance = new FileManager();
      Thread managerThread = new Thread(instance);
      managerThread.start();
    }
    return instance;
  }

  public ThreadPoolExecutor getThreadpool() {
    return threadpool;
  }

  public void write(WriteRequest request) throws IOException {
    PipedOutputStream outputStream = request.getPipe();
    Path path = Paths.get(request.getFilePath());

    try {
      AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE,
          StandardOpenOption.CREATE);
      ByteBuffer buffer = ByteBuffer.wrap(request.getContent());

      // Version chunk-a-chunk
      long position = request.getChunkNum() * Configuration.CHUNK_SIZE;

      fileChannel.write(buffer, position, buffer, new CompletionHandler<>() {
        @Override
        public void completed(Integer integer, ByteBuffer byteBuffer) {
          buffer.clear();
          try {
            fileChannel.close();
            outputStream.write(1);
          } catch (IOException e) {
            e.printStackTrace();
          }
          Logger.getGlobal().info("Successful chunk writing!");
        }

        @Override
        public void failed(Throwable throwable, ByteBuffer byteBuffer) {
          Logger.getGlobal().severe("Could not write chunk!");
        }
      });
    } catch (IOException e) {
      Logger.getGlobal().severe("File manager could not open write on path  " + path);
      outputStream.write(0);
      return;
    }
  }

  public void read(ReadRequest request) throws IOException {

    PipedOutputStream outputStream = request.getPipe();

    Path path = Paths.get(request.getFilePath());
    AsynchronousFileChannel fileChannel;
    try {
      fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
    } catch (IOException e) {
      Logger.getGlobal().severe("File manager could not open file on path  " + path);
      outputStream.write(0);
      return;
    }

    ByteBuffer buffer = ByteBuffer.allocate(Configuration.CHUNK_SIZE);
    long position = request.getChunkNum() * Configuration.CHUNK_SIZE;
    Future<Integer> operation = fileChannel.read(buffer, position);

    try {
      operation.get();
    } catch (Exception e) {
      Logger.getGlobal().severe("File manager could not read from path " + path);
      outputStream.write(0);
      return;
    }

    int chunkSize = buffer.position();
    outputStream.write(buffer.array(), 0, chunkSize);
  }

  public void delete(DeleteRequest request) throws IOException {
    PipedOutputStream outputStream = request.getPipe();
    Path path = Paths.get(request.getFilePath());
    try {
      Files.delete(path);
    } catch (IOException e) {
      Logger.getGlobal().severe("File manager could not delete from path " + path);
      outputStream.write(0);
      return;
    }
  }

  private void processRequest(Request request) throws Exception {
    if (request instanceof WriteRequest)
      this.write((WriteRequest) request);
    else if (request instanceof ReadRequest)
      this.read((ReadRequest) request);
    else if (request instanceof DeleteRequest)
      this.delete((DeleteRequest) request);
  }

  public void addRequest(Request request) {
    this.queue.add(request);
  }

  public CompletableFuture<byte[]> launchBackupReader(String fileName) {
    try {
      CompletableFuture<byte[]> fileFuture = new CompletableFuture<>();
      Reader reader = new Reader(fileName, fileFuture, Operation.BACKUP);
      threadpool.submit(reader);
      return fileFuture;
    } catch (IOException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public CompletableFuture<byte[]> launchRestoreReader(BigInteger fileId) {
    try {
      String fileName = fileId.toString();
      CompletableFuture<byte[]> fileFuture = new CompletableFuture<>();
      Reader reader = new Reader(fileName, fileFuture, Operation.RESTORE);
      threadpool.submit(reader);
      return fileFuture;
    } catch (IOException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void launchBackupWriter(BigInteger fileId, byte[] file) {
    try {
      String fileName = fileId.toString();
      Writer writer = new Writer(fileName, file, Operation.BACKUP);
      threadpool.submit(writer);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void launchRestoreWriter(String fileName, byte[] file) {
    try {
      Writer writer = new Writer(fileName, file, Operation.RESTORE);
      threadpool.submit(writer);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public ArrayList<BigInteger> getFilesToTransfer() {
    NodeInfo self = Node.get().getSelf();
    NodeInfo predecessor = Node.get().getPredecessor();

    List<String> result;
    try {
      Stream<Path> walk = Files.walk(Paths.get(BACKUP_FOLDER));
      result = walk.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());
    } catch(IOException e) {
      e.printStackTrace();
      return null;
    }

    ArrayList<BigInteger> filesToTranfer = new ArrayList<>();
    for (String fileName : result) {
      BigInteger fileKey = new BigInteger(fileName);
      if (!Chord.afterOrdered(predecessor.getChordId(),fileKey,self.getChordId())) {
        filesToTranfer.add(fileKey);
      }
    }
    return filesToTranfer;
  }

  @Override
  public void run() {
    Request request;

    while (true) {
      try {
        request = queue.poll(Configuration.REQUEST_WAITING_TIME, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        continue;
      }
      if (request == null)
        continue;
      try {
        this.processRequest(request);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
