package dbs.filesystem.threads;

import dbs.filesystem.Configuration;
import dbs.filesystem.FileManager;
import dbs.filesystem.messages.ReadRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Reader extends RequestManager implements Runnable {

  private final String key;
  private final CompletableFuture<byte[]> future;

  public Reader(String key, CompletableFuture<byte[]> future) throws IOException {
    super();
    this.key = key;
    this.future = future;
  }

  private ReadRequest createRequest(int chunkNum) {
    return new ReadRequest(this.key, chunkNum, this.outputStream);
  }

  @Override
  public void run() {

    int chunkNum = 0;
    int readBytes;
    ByteArrayOutputStream fileContent = new ByteArrayOutputStream();
    byte[] chunk = new byte[Configuration.CHUNK_SIZE];

    while(true) {
      FileManager.getInstance().addRequest(this.createRequest(chunkNum));
      try {
        readBytes = this.inputStream.read(chunk, 0, Configuration.CHUNK_SIZE);
        fileContent.write(chunk,0, readBytes);
      } catch (IOException e) {
        Logger.getGlobal().severe("Could not read from file with key " + this.key);
        this.closeStreams();
        return;
      }

      if(readBytes < Configuration.CHUNK_SIZE)
        break;
      Arrays.fill(chunk, (byte)0);
      chunkNum++;
    }
    Logger.getGlobal().info("Successful reading of file with key " + this.key);

    this.closeStreams();

    this.future.complete(fileContent.toByteArray());
  }
}
