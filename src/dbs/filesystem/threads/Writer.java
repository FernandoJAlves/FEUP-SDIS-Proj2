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

    for (int i = 0, j = 0; i < this.content.length; i+= Configuration.CHUNK_SIZE, j++)  {
      int chunkSize = this.content.length - i * Configuration.CHUNK_SIZE;
      byte[] chunkBuf = new byte[chunkSize];
      System.arraycopy(this.content,i,chunkBuf,0, chunkSize);

      FileManager.getInstance().addRequest(this.createRequest(j,chunkBuf));

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

      Arrays.fill(chunkBuf, (byte)0);
    }

    Logger.getGlobal().info("Successful writing of backed up file with key " + this.key);
    this.closeStreams();
  }
}
