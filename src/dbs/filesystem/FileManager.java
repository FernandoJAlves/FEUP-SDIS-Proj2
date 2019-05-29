package dbs.filesystem;

import dbs.filesystem.messages.DeleteRequest;
import dbs.filesystem.messages.ReadRequest;
import dbs.filesystem.messages.Request;
import dbs.filesystem.messages.WriteRequest;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
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
    if (instance == null)
      return new FileManager();
    return instance;
  }

  public void write(WriteRequest request) throws IOException {
    PipedOutputStream outputStream = request.getPipe();
    Path path = Paths.get(request.getFilePath());
    AsynchronousFileChannel fileChannel = null;
    try {
      fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);
    } catch (IOException e) {
      Logger.getGlobal().severe("File manager could not open write on path  " + path);
      outputStream.write(0);
      return;
    }
    ByteBuffer buffer = ByteBuffer.allocate(Configuration.CHUNK_SIZE);
    long position = request.getChunkNum() * Configuration.CHUNK_SIZE;
    buffer.put(request.getContent());
    fileChannel.write(buffer,position);
    buffer.clear();
  }

  public void read(ReadRequest request) throws IOException {

    PipedOutputStream outputStream = request.getPipe();

    Path path = Paths.get(request.getFilePath());
    AsynchronousFileChannel fileChannel = null;
    try {
      fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
    } catch(IOException e) {
      Logger.getGlobal().severe("File manager could not open file on path  " + path);
      outputStream.write(0);
      return;
    }

    ByteBuffer buffer = ByteBuffer.allocate(Configuration.CHUNK_SIZE);
    long position = request.getChunkNum() * Configuration.CHUNK_SIZE;
    Future<Integer> operation = fileChannel.read(buffer,position);

    try {
      operation.get();
    } catch (Exception e) {
      Logger.getGlobal().severe("File manager could not read from path " + path);
      outputStream.write(0);
      return;
    }
    byte[] chunkContent = new byte[Configuration.CHUNK_SIZE];
    buffer.get(chunkContent);
    outputStream.write(chunkContent,0, Configuration.CHUNK_SIZE);
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