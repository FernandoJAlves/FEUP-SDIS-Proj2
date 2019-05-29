package dbs.filesystem.messages;

import dbs.filesystem.Configuration;
import java.io.PipedOutputStream;

public class ReadRequest extends Request {

  private int chunkNum;

  public ReadRequest(String fileKey, int chunkNum, PipedOutputStream pipe) {
    super(fileKey, Configuration.Ownership.BACKEDUP, pipe);
    this.chunkNum = chunkNum;
  }

  public long getChunkNum() {
    return this.chunkNum;
  }
}
