package dbs.filesystem.threads;

import dbs.filesystem.Configuration;

public class Eraser implements Runnable {

  private final String path;

  public Eraser(Configuration.Ownership ownership, String key) {
    if(ownership == Configuration.Ownership.BACKEDUP)
      this.path = Configuration.BACKUP_FOLDER + key;
    else
      this.path = key;
  }

  @Override
  public void run() {

  }
}
