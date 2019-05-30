package dbs.filesystem;

import dbs.filesystem.messages.DeleteRequest;
import dbs.filesystem.messages.ReadRequest;
import dbs.filesystem.messages.Request;
import dbs.filesystem.messages.WriteRequest;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * The FileManager is used to manage access to the filesystem, providing read, write
 * and delete operations using the Java NIO API and a thread pool
 */
public class FileManager implements Runnable {

  private static FileManager instance;
  private final LinkedBlockingDeque<Request> queue;

  public FileManager() {
    this.queue = new LinkedBlockingDeque<>();
  }

  public static FileManager getInstance() {
    if (instance == null) {
      instance = new FileManager();
      Thread managerThread = new Thread(instance);
      managerThread.start();
    }
    return instance;
  }

  public void write(WriteRequest request) throws IOException {
    PipedOutputStream outputStream = request.getPipe();
    Path path = Paths.get(request.getFilePath());

    try {
      AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);
      ByteBuffer buffer = ByteBuffer.wrap(request.getContent());

      // Version chunk-a-chunk
      //long position = request.getChunkNum() * Configuration.CHUNK_SIZE;

      // Version full-file
      long position = 0;

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
    } catch(IOException e) {
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
    outputStream.write(buffer.array(),0, chunkSize);
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

  @Override
  public void run() {
    Request request;

    while (true) {
      try {
        request = queue.poll(Configuration.REQUEST_WAITING_TIME, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        continue;
      }
      if (request == null) continue;
      try {
        this.processRequest(request);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}