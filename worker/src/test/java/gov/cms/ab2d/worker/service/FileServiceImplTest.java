package gov.cms.ab2d.worker.service;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.UncheckedIOException;

class FileServiceImplTest {

  @Test
  void testGenerateChecksum() {
    FileServiceImpl fileServiceImpl = new FileServiceImpl();
    File file = new File("src/test/resources/file.txt");
    String checksum = fileServiceImpl.generateChecksum(file);
    assertEquals("a23e5fdcd7b276bdd81aa1a0b7b963101863dd3f61ff57935f8c5ba462681ea6", checksum);
  }

  @Test
  void testGenerateChecksumError() {
    FileServiceImpl fileServiceImpl = new FileServiceImpl();
    File file = new File("src/test/resources/does-not-exist.txt");
    assertThrows(UncheckedIOException.class, () -> {
      fileServiceImpl.generateChecksum(file);
    });
  }

}
