package dbs.filesystem.messages;

import dbs.filesystem.Configuration;

import java.io.PipedOutputStream;

public abstract class Request {

  protected String filePath;
  protected PipedOutputStream pipe;

  public Request(String fileKey, Configuration.Ownership ownership, PipedOutputStream pipe) {
    if(ownership == Configuration.Ownership.BACKEDUP)
      this.filePath = Configuration.BACKUP_FOLDER + fileKey;
    else
      this.filePath = fileKey;
    this.pipe = pipe;
  }

  public String getFilePath() {
    return this.filePath;
  }

  public PipedOutputStream getPipe() {
    return this.pipe;
  }
}
