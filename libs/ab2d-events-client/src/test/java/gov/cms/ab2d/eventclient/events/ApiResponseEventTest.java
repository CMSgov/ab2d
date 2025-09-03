package gov.cms.ab2d.eventclient.events;


import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;

class ApiResponseEventTest {

  ApiResponseEvent apiResponseEvent;

  @Test
  void testConstructor() {
    assertDoesNotThrow(() -> {
      new ApiResponseEvent(null, null, null, null, null, null);
      new ApiResponseEvent("CMS", "1234", HttpStatus.OK, "OK", "OK", "1234");
    });
  }

  @Test
  void testAsMessage() {
    apiResponseEvent = new ApiResponseEvent();
    apiResponseEvent.setResponseCode(200);
    apiResponseEvent.setDescription("OK");
    assertEquals("(200): OK", apiResponseEvent.asMessage());
  }

}
