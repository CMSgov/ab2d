package gov.cms.ab2d.eventclient.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ApiRequestEventTest {

  ApiRequestEvent apiRequestEvent;

  @Test
  void testContructor() {
    assertDoesNotThrow(
      () -> {
        new ApiRequestEvent(null, null, null, null, null, null);
        new ApiRequestEvent("CMS", "1234", "http://localhost", "1.1.1.1", "1234", "1234");
      }
    );
  }

  @Test
  void testAsMessage() {
    apiRequestEvent = new ApiRequestEvent();
    apiRequestEvent.setUrl("http://localhost");
    apiRequestEvent.setIpAddress("1.1.1.1");
    assertEquals("request to http://localhost from 1.1.1.1", apiRequestEvent.asMessage());
  }

  @Test
  void testHashIt() {
    assertNull(ApiRequestEvent.hashIt(null));
    assertNotNull(ApiRequestEvent.hashIt("secret"));
  }

}
