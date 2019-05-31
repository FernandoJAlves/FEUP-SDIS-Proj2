package dbs.filesystem.threads;

import dbs.filesystem.Configuration;
import dbs.filesystem.FileManager;
import dbs.filesystem.messages.DeleteRequest;

import java.io.IOException;

public class Eraser extends RequestManager implements Runnable {

  private final String key;

  public Eraser(String key) throws IOException {
    super();
    this.key = key;
  }

  private DeleteRequest createRequest() {
    String filepath = Configuration.BACKUP_FOLDER + this.key;
    return new DeleteRequest(filepath, this.outputStream);
  }
  
  @Override
  public void run() {
    FileManager.getInstance().addRequest(this.createRequest());
    int response = 1;
    try {
      response = this.inputStream.read();
    } catch (IOException e) {
      response = 0;
    }

    /*if(response == 0)
        Logger.getGlobal().severe("Could not delete " + this.ownership.name() + " file with key " + this.key);
      else if (response == 1)
        Logger.getGlobal().info("Successful deletion of " + this.ownership.name() + " file with key " + this.key);
     */

    this.closeStreams();
  }
}
