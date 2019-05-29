import dbs.filesystem.Configuration;
import dbs.filesystem.threads.Eraser;
import dbs.filesystem.threads.Writer;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class TestFilesystem {

  @Test
  public void testDelete() throws Exception {

    // Generate test file
    String path = "/tmp/test_file.txt";
    File file = new File(path);

    // Delete previous file (if it exists)
    file.delete();

    // Check if file was generated
    TestCase.assertTrue(file.createNewFile());

    // Launch thread
    Eraser eraser = new Eraser(path, Configuration.Ownership.OWNED);
    Thread eraserThread = new Thread(eraser);
    eraserThread.start();

    // Check if file was deleted
    TimeUnit.MILLISECONDS.sleep(100);
    TestCase.assertFalse(file.exists());
  }

  @Test
  public void testWrite() throws Exception {

    // Generate test file
    String fileKey = "test_file.txt";
    String path = Configuration.BACKUP_FOLDER + fileKey;
    File file = new File(path);

    // Delete previous file (if it exists)
    file.delete();

    // Check if file was generated
    file.getParentFile().mkdirs();
    TestCase.assertTrue(file.createNewFile());

    // File's content to be written
    byte[] fileContent = "This is a test message!".getBytes();

    // Write content to file
    Writer writer = new Writer(fileKey, fileContent);
    Thread writerThread = new Thread(writer);
    writerThread.start();

    // Check if file content matches
    TimeUnit.MILLISECONDS.sleep(100);
    byte[] writtenContent = Files.readAllBytes(Paths.get(path));
    Assert.assertArrayEquals(fileContent, writtenContent);
  }
}
