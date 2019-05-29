package dbs.filesystem.threads;

import dbs.filesystem.Configuration;
import dbs.filesystem.FileManager;
import dbs.filesystem.messages.DeleteRequest;

import java.io.IOException;
import java.util.logging.Logger;

public class Eraser extends RequestManager implements Runnable {

  private final String key;
  private final Configuration.Ownership ownership;

  public Eraser(String key,Configuration.Ownership ownership) throws IOException {
    super();
    this.key = key;
    this.ownership = ownership;
  }

  private DeleteRequest createRequest() {
    return new DeleteRequest(this.key,this.ownership, this.outputStream);
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

    if(response == 0)
      Logger.getGlobal().severe("Could not delete " + this.ownership.name() + " file with key " + this.key);
    else if (response == 1)
      Logger.getGlobal().info("Successful deletion of " + this.ownership.name() + " file with key " + this.key);

    this.closeStreams();
  }
}
