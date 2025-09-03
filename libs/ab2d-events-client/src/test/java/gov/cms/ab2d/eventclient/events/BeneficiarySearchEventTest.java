package gov.cms.ab2d.eventclient.events;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class BeneficiarySearchEventTest {

  BeneficiarySearchEvent beneficiarySearchEvent;

  @Test
  void testConstructor() {
    assertDoesNotThrow(() -> {
      new BeneficiarySearchEvent(null, null, null, null, null, null, null);
      new BeneficiarySearchEvent("CMS", "1234", "1234", OffsetDateTime.now(), OffsetDateTime.now(), 99L, "OK");
    });
  }

  @Test
  void testAsMessage() {
    beneficiarySearchEvent = new BeneficiarySearchEvent();
    beneficiarySearchEvent.setJobId("1234");
    beneficiarySearchEvent.setContractNum("1234");
    beneficiarySearchEvent.setResponse("OK");
    assertEquals("(1234) bene search 1234 response OK", beneficiarySearchEvent.asMessage());
  }

}
