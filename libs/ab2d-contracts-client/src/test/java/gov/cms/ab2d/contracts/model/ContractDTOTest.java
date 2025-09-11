package gov.cms.ab2d.contracts.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContractDTOTest {

  @Test
  void testHasDateIssueCaseOne() {
    ContractDTO contractDTO = new ContractDTO();
    contractDTO.setContractType(Contract.ContractType.CLASSIC_TEST);
    assertTrue(contractDTO.hasDateIssue());
  }

  @Test
  void testHasDateIssueCaseTwo() {
    ContractDTO contractDTO = new ContractDTO();
    contractDTO.setContractType(Contract.ContractType.NORMAL);
    assertFalse(contractDTO.hasDateIssue());
  }

}
