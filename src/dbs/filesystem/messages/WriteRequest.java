package dbs.filesystem.messages;

import dbs.filesystem.Configuration;

import java.io.PipedOutputStream;

public class WriteRequest extends Request {

  private int chunkNum;
  private byte[] content;

  public WriteRequest(String fileKey, int chunkNum, byte[] content, PipedOutputStream pipe) {
    super(fileKey, Configuration.Ownership.BACKEDUP, pipe);
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
