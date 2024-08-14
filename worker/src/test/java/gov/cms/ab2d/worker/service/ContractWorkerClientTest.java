package gov.cms.ab2d.worker.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import gov.cms.ab2d.common.service.ContractService;

class ContractWorkerClientTest {

  @Test
  void testGetContractByContractNumber() {
    ContractService contractService = mock(ContractService.class);
    ContractWorkerClient contractWorkerClient = new ContractWorkerClient(contractService);
    contractWorkerClient.getContractByContractNumber("1234");
    verify(contractService).getContractByContractNumber("1234");
  }

}
