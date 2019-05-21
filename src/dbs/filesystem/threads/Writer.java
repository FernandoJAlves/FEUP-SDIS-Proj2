package dbs.filesystem.threads;

public class Writer implements Runnable {

  private final String key;
  private final byte[] content;

  public Writer(String key, byte[] content) {
    this.key = key;
    this.content = content;
  }

  @Override
  public void run() {

  }
}
