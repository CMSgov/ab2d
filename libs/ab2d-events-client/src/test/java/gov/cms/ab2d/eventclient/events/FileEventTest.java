package gov.cms.ab2d.eventclient.events;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class FileEventTest {

  FileEvent fileEvent;

  @Test
  void testConstructor() {
    assertDoesNotThrow(() -> {
      new FileEvent(null, null, null, null);
      new FileEvent(
        "CMS", "1234", new File("src/test/resources/application.properties"), FileEvent.FileStatus.OPEN
      );
      new FileEvent(
        "CMS", "1234", new File("does-not-exist"), FileEvent.FileStatus.OPEN
      );
    });
  }

  @Test
  void testAsMessage() {
    fileEvent = new FileEvent();
    fileEvent.setJobId("1234");
    fileEvent.setStatus(FileEvent.FileStatus.OPEN);
    fileEvent.setFileName("/var/whatever");
    assertEquals("(1234): OPEN /var/whatever", fileEvent.asMessage());
  }

}
