package gov.cms.ab2d.eventclient.events;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ContractSearchEventTest {

  ContractSearchEvent contractSearchEvent;

  @Test
  void testConstructor() {
    assertDoesNotThrow(() -> {
      new ContractSearchEvent(null, null, null, 0, 0, 0, 0, 0, 0, 0, 0);
      new ContractSearchEvent("CMS", "1234", "1234", 99, 99, 99, 99, 99, 99, 99, 99);
    });
  }

  @Test
  void testAsMessage() {
    contractSearchEvent = new ContractSearchEvent();
    contractSearchEvent.setJobId("1234");
    contractSearchEvent.setContractNumber("1234");
    contractSearchEvent.setBenesExpected(99);
    assertEquals("(1234) 1234 number in contract 99", contractSearchEvent.asMessage());
  }

}
