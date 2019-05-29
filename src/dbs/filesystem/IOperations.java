package dbs.filesystem;

import dbs.filesystem.messages.DeleteRequest;
import dbs.filesystem.messages.ReadRequest;
import dbs.filesystem.messages.WriteRequest;

public interface IOperations {
  /**
   * @param request A request to write to a file
   */
  void write(WriteRequest request);

  /**
   * @param request A request to read from a file
   */
  void read(ReadRequest request);

  /**
   * @param request A request to delete a file
   */
  void delete(DeleteRequest request);
}