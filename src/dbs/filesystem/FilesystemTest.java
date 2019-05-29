package dbs.filesystem;

import dbs.filesystem.threads.Eraser;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class FilesystemTest {

  @Test
  public void testDelete() throws Exception {

    // Generate test file
    String path = "/tmp/test_file.txt";
    File file = new File(path);

    // Delete previous file (if it exists)
    file.delete();

    // Check if file was generated
    assertTrue(file.createNewFile());

    // Launch thread
    Eraser eraser = new Eraser(path, Configuration.Ownership.OWNED);
    Thread eraserThread = new Thread(eraser);
    eraserThread.start();

    // Check if file was deleted
    TimeUnit.SECONDS.sleep(1);
    assertFalse(file.exists());
    // TODO: continue here
  }

}
