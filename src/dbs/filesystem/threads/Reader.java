package dbs.filesystem.threads;

import java.util.concurrent.Callable;

public class Reader implements Callable<byte[]> {

  private final String key;

  public Reader(String key) {
    this.key = key;
  }

  @Override
  public byte[] call() throws Exception {
    return new byte[0];
  }
}
