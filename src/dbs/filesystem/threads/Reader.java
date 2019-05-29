package dbs.filesystem.threads;

import dbs.chord.NodeInfo;
import dbs.filesystem.Configuration;
import dbs.filesystem.FileManager;
import dbs.filesystem.messages.ReadRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

public class Reader extends RequestManager implements Runnable {

  private final String key;
  private NodeInfo destinationNode;

  public Reader(String key, NodeInfo destinationNode) throws IOException {
    super();
    this.key = key;
    this.destinationNode = destinationNode;
  }

  private ReadRequest createRequest(int chunkNum) {
    return new ReadRequest(this.key, chunkNum, this.outputStream);
  }

  @Override
  public void run() {

    int chunkNum = 0;
    int readBytes = 0;
    ByteArrayOutputStream fileContent = new ByteArrayOutputStream();
    byte[] chunk = new byte[Configuration.CHUNK_SIZE];

    while(true) {
      FileManager.getInstance().addRequest(this.createRequest(chunkNum));
      try {
        readBytes = this.inputStream.read(chunk, 0, Configuration.CHUNK_SIZE);
        fileContent.write(chunk);
      } catch (IOException e) { // TODO: better approach
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

    byte[] fileContentBuf = fileContent.toByteArray();
    this.closeStreams();

    // TODO: send file content to destination node
  }

}
