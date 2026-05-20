package gov.cms.ab2d.eventclient.events;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class JobSummaryEventTest {

  private JobSummaryEvent jobSummaryEvent;
  private OffsetDateTime now = OffsetDateTime.now();

  @Test
  void testAsMessage() {
    jobSummaryEvent = new JobSummaryEvent();
    jobSummaryEvent.setJobId("1234");
    jobSummaryEvent.setSubmittedTime(now);
    jobSummaryEvent.setSuccessfullySearched(99);
    assertEquals(String.format("(1234) submitted at %s successfully searched 99", now.toString()), jobSummaryEvent.asMessage());
  }

}
