package dbs.filesystem.messages;

import java.io.PipedOutputStream;

public class DeleteRequest extends Request {

  public DeleteRequest(String filePath, PipedOutputStream pipe) {
    super(filePath, pipe);
  }
}