package gov.cms.ab2d.eventclient.events;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;

class LoggableEventTest {

  // LoggableEvent is abstract, so we use a subclass instead
  class MockLoggableEvent extends LoggableEvent {
    public String asMessage() {
      return "";
    }
  }

  @Test
  void testSetEnvironment() {
    LoggableEvent event = new MockLoggableEvent();

    event.setEnvironment("example");
    assertEquals("example", event.getEnvironment());

    event.setEnvironment(Ab2dEnvironment.LOCAL);
    assertEquals("local", event.getEnvironment());
  }

  @Test
  void testHashCodeCaseOne() {
    LoggableEvent event = new MockLoggableEvent();
    assertEquals(437864549, event.hashCode());
  }

  @Test
  void testHashCodeCaseTwo() {
    LoggableEvent event = new MockLoggableEvent();
    event.setEnvironment("example");
    event.setId(99L);
    event.setAwsId("AWS");
    event.setTimeOfEvent(OffsetDateTime.of(1, 1, 1, 1, 1, 1, 1, ZoneOffset.of("Z")));
    event.setOrganization("CMS");
    event.setJobId("1234");
    assertEquals(1726282058, event.hashCode());
  }

  @Test
  void testEqualsCaseOne() {
    LoggableEvent event = new MockLoggableEvent();
    assertEquals(true, event.equals(event));
  }

  @Test
  void testEqualsCaseTwo() {
    LoggableEvent event = new MockLoggableEvent();
    Object other = new LoggableEventTest();
    assertEquals(false, event.equals(other));
  }

  // testEqualsEnvironment //

  @Test
  void testEqualsEnvironmentCaseOne() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    other.setEnvironment("example");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsEnvironmentCaseTwo() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setEnvironment("example");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsEnvironmentCaseThree() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setEnvironment("example1");
    other.setEnvironment("example2");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsEnvironmentCaseFour() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setEnvironment("example");
    other.setEnvironment("example");
    assertEquals(true, event.equals(other));
  }

  // testEqualsId //

  @Test
  void testEqualsIdCaseOne() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    other.setId(99L);
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsIdCaseTwo() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setId(99L);
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsIdCaseThree() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setId(1L);
    other.setId(2L);
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsIdCaseFour() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setId(99L);
    other.setId(99L);
    assertEquals(true, event.equals(other));
  }

  // testEqualsAwsId //

  @Test
  void testEqualsAwsIdCaseOne() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    other.setAwsId("AWS");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsAwsIdCaseTwo() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setAwsId("AWS");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsAwsIdCaseThree() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setAwsId("AWS1");
    other.setAwsId("AWS2");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsAwsIdCaseFour() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setAwsId("AWS");
    other.setAwsId("AWS");
    assertEquals(true, event.equals(other));
  }

  // testEqualsTime //

  @Test
  void testEqualsTimeCaseOne() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    other.setTimeOfEvent(OffsetDateTime.of(1, 1, 1, 1, 1, 1, 1, ZoneOffset.of("Z")));
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsTimeCaseTwo() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setTimeOfEvent(OffsetDateTime.of(1, 1, 1, 1, 1, 1, 1, ZoneOffset.of("Z")));
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsTimeCaseThree() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setTimeOfEvent(OffsetDateTime.of(1, 1, 1, 1, 1, 1, 1, ZoneOffset.of("Z")));
    other.setTimeOfEvent(OffsetDateTime.of(9, 9, 9, 9, 9, 9, 9, ZoneOffset.of("Z")));
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsTimeCaseFour() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setTimeOfEvent(OffsetDateTime.of(1, 1, 1, 1, 1, 1, 1, ZoneOffset.of("Z")));
    other.setTimeOfEvent(OffsetDateTime.of(1, 1, 1, 1, 1, 1, 1, ZoneOffset.of("Z")));
    assertEquals(true, event.equals(other));
  }

  // testEqualsOrg //

  @Test
  void testEqualsOrgCaseOne() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    other.setOrganization("CMS");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsOrgCaseTwo() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setOrganization("CMS");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsOrgCaseThree() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setOrganization("CMS1");
    other.setOrganization("CMS2");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsOrgCaseFour() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setOrganization("CMS");
    other.setOrganization("CMS");
    assertEquals(true, event.equals(other));
  }

  // testEqualsJob //

  @Test
  void testEqualsJobCaseOne() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    other.setJobId("1234");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsJobCaseTwo() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setJobId("1234");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsJobCaseThree() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setJobId("1234");
    other.setJobId("9999");
    assertEquals(false, event.equals(other));
  }

  @Test
  void testEqualsJobCaseFour() {
    LoggableEvent event = new MockLoggableEvent();
    LoggableEvent other = new MockLoggableEvent();
    event.setJobId("1234");
    other.setJobId("1234");
    assertEquals(true, event.equals(other));
  }

}
