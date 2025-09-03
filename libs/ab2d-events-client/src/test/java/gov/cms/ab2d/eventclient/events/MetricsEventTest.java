package gov.cms.ab2d.eventclient.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class MetricsEventTest {

  @Test
  void testEqualsCaseOne() {
    MetricsEvent event = new MetricsEvent();
    assertEquals(true, event.equals(event));
  }

  @Test
  void testEqualsCaseTwo() {
    MetricsEvent event = new MetricsEvent();
    Object other = new MetricsEventTest();
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsCaseThree() {
    MetricsEvent event = new MetricsEvent();
    event.setId(99L);
    event.setService("service");
    event.setStateType(MetricsEvent.State.CONTINUE);
    event.setEventDescription("description");

    MetricsEvent other = new MetricsEvent();
    other.setId(99L);
    other.setService("service");
    other.setStateType(MetricsEvent.State.CONTINUE);
    other.setEventDescription("description");

    assertEquals(true, event.equals(other));
  }

  @Test
  void testEqualsCaseFour() {
    MetricsEvent event = new MetricsEvent();
    event.setId(1L);

    MetricsEvent other = new MetricsEvent();
    other.setId(2L);

    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsCaseFive() {
    MetricsEvent event = new MetricsEvent();
    event.setService("service");
    event.setStateType(MetricsEvent.State.CONTINUE);
    event.setEventDescription("description");

    MetricsEvent other = new MetricsEvent();
    other.setService("service");
    other.setStateType(MetricsEvent.State.CONTINUE);
    other.setEventDescription("description");

    assertEquals(true, event.equals(other));
  }

  @Test
  void testEqualsCaseSix() {
    MetricsEvent event = new MetricsEvent();
    event.setService("service1");
    event.setStateType(MetricsEvent.State.CONTINUE);

    MetricsEvent other = new MetricsEvent();
    other.setService("service2");
    other.setStateType(MetricsEvent.State.END);

    assertEquals(false, event.equals(other));
  }

  @Test
  void testContructor() {
    assertDoesNotThrow(() -> {
      new MetricsEvent(
        "service",
        "description",
        OffsetDateTime.of(1, 1, 1, 1, 1, 1, 1, ZoneOffset.of("Z")),
        MetricsEvent.State.CONTINUE
      );
    });
  }

  @Test
  void testAsMessage() {
    MetricsEvent event = new MetricsEvent();
    event.setService("service");
    event.setStateType(MetricsEvent.State.CONTINUE);
    assertEquals("(service) CONTINUE", event.asMessage());
  }

}
