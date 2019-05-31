package dbs.filesystem.messages;

import java.io.PipedOutputStream;

public abstract class Request {

  protected String filePath;
  protected PipedOutputStream pipe;

  public Request(String filePath, PipedOutputStream pipe) {
    this.filePath = filePath;
    this.pipe = pipe;
  }

  public String getFilePath() {
    return this.filePath;
  }

  public PipedOutputStream getPipe() {
    return this.pipe;
  }
}
