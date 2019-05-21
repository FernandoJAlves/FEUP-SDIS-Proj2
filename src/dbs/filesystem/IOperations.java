package dbs.filesystem;

import java.util.concurrent.ExecutionException;

public interface IOperations {
  /**
   * Writes a new file to the filesystem
   * @param operation Either BACKUP or RESTORE
   * @param key The file's key (to be used as the file's name)
   * @param content The content to be written to the file
   */
  void write(Configuration.Operation operation, String key, byte[] content);

  /**
   * Reads a file from the filesystem
   * @param key The key of the file to be read
   * @return The content of the file
   * @throws ExecutionException
   * @throws InterruptedException
   */
  byte[] read(String key) throws ExecutionException, InterruptedException;

  /**
   * Deletes a file from the filesystem
   * @param ownership Either OWNED or BACKEDUP
   * @param key The file's path (when OWNED) or the file's key (when BACKEDUP)
   */
  void delete(Configuration.Ownership ownership, String key);
}