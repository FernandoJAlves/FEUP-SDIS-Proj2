package dbs.filesystem.messages;

import java.io.PipedOutputStream;

public class ReadRequest extends Request {

  private int chunkNum;

  public ReadRequest(String filePath, int chunkNum, PipedOutputStream pipe) {
    super(filePath, pipe);
    this.chunkNum = chunkNum;
  }

  public long getChunkNum() {
    return this.chunkNum;
  }
}
