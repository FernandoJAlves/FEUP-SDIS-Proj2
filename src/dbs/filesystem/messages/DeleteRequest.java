package dbs.filesystem.messages;

import dbs.filesystem.Configuration;

import java.io.PipedOutputStream;

public class DeleteRequest extends Request {

  public DeleteRequest(String fileKey, Configuration.Ownership ownership, PipedOutputStream pipe) {
    super(fileKey, ownership, pipe);
  }
}