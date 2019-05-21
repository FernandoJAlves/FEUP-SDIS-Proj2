package dbs.filesystem;

import dbs.filesystem.threads.Eraser;
import dbs.filesystem.threads.Reader;
import dbs.filesystem.threads.Writer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The FileManager is used to manage access to the filesystem, providing read, write
 * and delete operations using the Java NIO API and a thread pool
 */
public class FileManager implements IOperations {

  /**
   * Threadpool's number of threads
   */
  private final int POOL_SIZE = 5;

  /**
   * Threadpool initialization
   */
  private ExecutorService threadPool = Executors.newFixedThreadPool(POOL_SIZE);

  @Override
  public void write(Configuration.Operation operation, String key, byte[] content) {
    threadPool.execute(new Writer(key, content));
  }

  @Override
  public byte[] read(String key) throws ExecutionException, InterruptedException {
    Reader reader = new Reader(key);
    Future<byte[]> future = threadPool.submit(reader);
    return future.get();
  }

  @Override
  public void delete(Configuration.Ownership ownership, String key) {
    threadPool.execute(new Eraser(ownership, key));
  }

}