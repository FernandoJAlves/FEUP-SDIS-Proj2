package dbs.filesystem.messages;

import dbs.filesystem.Configuration;

import java.io.PipedOutputStream;

public class WriteRequest extends Request {

  private int chunkNum;
  private byte[] content;

  public WriteRequest(String filePath, int chunkNum, byte[] content, PipedOutputStream pipe) {
    super(filePath,pipe);
    this.chunkNum = chunkNum;
    this.content = content;
  }

  public int getChunkNum() {
    return this.chunkNum;
  }

  public byte[] getContent() {
    return this.content;
  }
}
