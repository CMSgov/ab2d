package gov.cms.ab2d.eventclient.events;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ReloadEventTest {

  ReloadEvent reloadEvent;

  @Test
  void testConstructor() {
    assertDoesNotThrow(() -> {
      new ReloadEvent(null, null, null, 0);
      new ReloadEvent("CMS", ReloadEvent.FileType.OPT_OUT, "fileName", 99);
    });
  }

  @Test
  void testAsMessage() {
    reloadEvent = new ReloadEvent();
    reloadEvent.setJobId("1234");
    reloadEvent.setFileType(ReloadEvent.FileType.OPT_OUT);
    reloadEvent.setFileName("fileName");
    assertEquals("(1234) OPT_OUT fileName", reloadEvent.asMessage());
  }

}
