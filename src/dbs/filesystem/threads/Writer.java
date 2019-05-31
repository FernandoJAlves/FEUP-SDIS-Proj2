package dbs.filesystem.threads;

import dbs.filesystem.Configuration;
import dbs.filesystem.FileManager;
import dbs.filesystem.messages.WriteRequest;
import java.io.IOException;

public class Writer extends RequestManager implements Runnable {

  private final String key;
  private final byte[] content;
  private final Configuration.Operation operation;

  public Writer(String key, byte[] content, Configuration.Operation operation) throws IOException {
    super();
    this.key = key;
    this.content = content;
    this.operation = operation;
  }

  private WriteRequest createRequest(int chunkNum, byte[] content) {
    String filePath = null;
    if (this.operation == Configuration.Operation.BACKUP) {
      filePath = FileManager.BACKUP_FOLDER + this.key;
    } else if (this.operation == Configuration.Operation.RESTORE) {
      filePath = FileManager.RESTORE_FOLDER + this.key;
    }
    assert filePath != null;
    return new WriteRequest(filePath, chunkNum, content, this.outputStream);
  }

  @Override
  public void run() {

    for (int i = 0, j = 0; i < this.content.length; i+= Configuration.CHUNK_SIZE, j++)  {
      int chunkSize = Configuration.CHUNK_SIZE;
      if (this.content.length - i < Configuration.CHUNK_SIZE) {
        chunkSize = this.content.length - i;
      }
      byte[] chunkBuf = new byte[chunkSize];
      System.arraycopy(this.content,i,chunkBuf,0, chunkSize);

      FileManager.getInstance().addRequest(this.createRequest(j,chunkBuf));
      //Logger.getGlobal().info("Writing chunk " + j + " of size " + chunkSize);

      int response;
      try {
        response = this.inputStream.read();
      } catch (IOException e) {
        response = 0;
      }
      if(response == 0) {
        //Logger.getGlobal().severe("Could not write backed up file with key " + this.key);
        this.closeStreams();
        return;
      }
    }

    //Logger.getGlobal().info("Successful writing of backed up file with key " + this.key);
    this.closeStreams();

  }
}
