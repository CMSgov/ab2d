package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.worker.processor.domainmodel.ContractMapping;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled
@SpringBootTest
class PatientContractCallableLiveTest {

    @Autowired
    BFDClient bfdClient;

    @DisplayName("Successfully completing marks as done and transfers results")
    @Test
    void callableFunctions() {

        Contract contract = new Contract();
        contract.setContractNumber("Z0001");
        contract.setContractName("Z0001");

        PatientContractCallable callable = new PatientContractCallable("Z0001", 1, 3, bfdClient);

        try {
            ContractMapping results = callable.call();

            assertFalse(results.getPatients().isEmpty());
        } catch (Exception exception) {
            fail("could not execute against sandbox", exception);
        }
    }
}
