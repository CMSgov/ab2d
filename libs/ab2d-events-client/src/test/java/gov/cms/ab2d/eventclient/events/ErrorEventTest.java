package gov.cms.ab2d.eventclient.events;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ErrorEventTest {

  ErrorEvent errorEvent;

  @Test
  void testConstructor() {
    assertDoesNotThrow(() -> {
      new ErrorEvent(null, null, null, null);
      new ErrorEvent("CMS", "1234", ErrorEvent.ErrorType.FILE_ALREADY_DELETED, "ERROR");
    });
  }

  @Test
  void testAsMessage() {
    errorEvent = new ErrorEvent();
    errorEvent.setJobId("1234");
    errorEvent.setErrorType(ErrorEvent.ErrorType.FILE_ALREADY_DELETED);
    errorEvent.setDescription("ERROR");
    assertEquals("(1234): FILE_ALREADY_DELETED ERROR", errorEvent.asMessage());
  }

}
