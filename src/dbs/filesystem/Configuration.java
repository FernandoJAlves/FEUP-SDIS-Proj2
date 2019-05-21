package dbs.filesystem;

public class Configuration {

  /**
   * Attribute used to identify the type of write operation. Either writing a
   * backed file or a restored file
   */
  public enum Operation {BACKUP, RESTORE}
  /**
   * Attribute used to identify a file and its owner. This is useful in the
   * case of a delete operation, to obtain the desired file path, since a
   * file can be deleted if it was previously backed up or originally
   * owned by the peer.
   * Assumptions regarding the keys:
   * OWNED -> The file's path
   * BACKEDUP -> The file's hash
   */
  public enum Ownership {OWNED, BACKEDUP}
  /**
   * The directory where the backed-up files are stored
   */
  public static final String BACKUP_FOLDER = "/tmp/dbs/backup";
  /**
   * The directory where the restored files are stored
   */
  public static final String RESTORE_FOLDER = "/tmp/dbs/restore";
}