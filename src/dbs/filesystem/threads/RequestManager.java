package dbs.filesystem.threads;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Logger;

public class RequestManager {

  protected PipedOutputStream outputStream;
  protected PipedInputStream inputStream;

  protected RequestManager() throws IOException {
    this.outputStream = new PipedOutputStream();
    this.inputStream = new PipedInputStream(this.outputStream);
  }

  protected void closeStreams() {
    try {
      this.outputStream.close();
      this.inputStream.close();
    } catch (IOException e) {
      Logger.getGlobal().severe("Could not close the pipe streams");
    }
  }
}

