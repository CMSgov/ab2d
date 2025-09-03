package gov.cms.ab2d.eventclient.events;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

class SlackEventsTest {

  @Test
  void testEnum() {
    // We're just looking to get test coverage on the enum here.
    // We don't even really need to "test" anything of significance.
    assertEquals("API_AUTHNZ_ERROR", SlackEvents.API_AUTHNZ_ERROR.toString());
  }

}
