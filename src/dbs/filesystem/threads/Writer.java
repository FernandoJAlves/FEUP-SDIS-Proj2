package dbs.filesystem.threads;

import dbs.filesystem.Configuration;
import dbs.filesystem.FileManager;
import dbs.filesystem.messages.WriteRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

public class Writer extends RequestManager implements Runnable {

  private final String key;
  private final byte[] content;

  public Writer(String key, byte[] content) throws IOException {
    super();
    this.key = key;
    this.content = content;
  }

  private WriteRequest createRequest(int chunkNum, byte[] content) {
    return new WriteRequest(this.key, chunkNum, content, this.outputStream);
  }

  @Override
  public void run() {

    // Version chunk-a-chunk
    for (int i = 0, j = 0; i < this.content.length; i+= Configuration.CHUNK_SIZE, j++)  {
      int chunkSize = Configuration.CHUNK_SIZE;
      if (this.content.length - i < Configuration.CHUNK_SIZE) {
        chunkSize = this.content.length - i;
      }
      byte[] chunkBuf = new byte[chunkSize];
      System.arraycopy(this.content,i,chunkBuf,0, chunkSize);

      FileManager.getInstance().addRequest(this.createRequest(j,chunkBuf));
      Logger.getGlobal().info("Writing chunk " + j + " of size " + chunkSize);

      int response;
      try {
        response = this.inputStream.read();
      } catch (IOException e) {
        response = 0;
      }
      if(response == 0) {
        Logger.getGlobal().severe("Could not write backed up file with key " + this.key);
        this.closeStreams();
        return;
      }
    }

    /* Version full-file
    FileManager.getInstance().addRequest(this.createRequest(0,this.content));

    int response;
    try {
      response = this.inputStream.read();
    } catch (IOException e) {
      response = 0;
    }
    if(response == 0) {
      Logger.getGlobal().severe("Could not write backed up file with key " + this.key);
      this.closeStreams();
      return;
    }
    */

    Logger.getGlobal().info("Successful writing of backed up file with key " + this.key);
    this.closeStreams();
  }
}
