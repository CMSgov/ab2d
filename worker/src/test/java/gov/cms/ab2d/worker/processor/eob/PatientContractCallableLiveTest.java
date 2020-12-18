package gov.cms.ab2d.worker.processor.eob;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneSearchImpl;
import gov.cms.ab2d.worker.processor.coverage.ContractMapping;
import gov.cms.ab2d.worker.processor.eob.PatientContractCallable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live test meant to be run locally against the BFD Prod sandbox. Test assumes that you are running a Postgres
 * database locally exposed on a port
 *
 * You need all of the following environment variables
 * to run this test
 *
 * AB2D_HICN_HASH_PEPPER
 * AB2D_HICN_HASH_ITER=1000
 * AB2D_CLAIMS_SKIP_BILLABLE_PERIOD_CHECK
 * DB_URL=jdbc:postgresql://localhost:5432/ab2d (port is machine dependent)
 * DB_USERNAME=ab2d
 * DB_PASSWORD=ab2d
 * AB2D_BFD_KEYSTORE_LOCATION (machine dependent)
 * AB2D_BFD_KEYSTORE_PASSWORD
 *
 * Change the bundle size to test for edge cases in the BFD api related
 * to the last bundle.
 */
@Disabled
@SpringBootTest(properties = {"bfd.contract.to.bene.pagesize=500"})
class PatientContractCallableLiveTest {

    public static final String TESTING_JOB_ID = "TESTING_JOB_ID";

    @Autowired
    ContractBeneSearchImpl contractBeneSearch;

    @Autowired
    BFDClient bfdClient;

    @DisplayName("With Year C.E. 3 returns all data available")
    @Test
    void findAll() {

        Contract contract = new Contract();
        contract.setContractNumber("Z0001");
        contract.setContractName("Z0001");

        PatientContractCallable callable = new PatientContractCallable("Z0001", 1, 3, bfdClient,
                true, TESTING_JOB_ID);

        try {
            ContractMapping results = callable.call();

            assertFalse(results.getPatients().isEmpty());
            assertEquals(1000, results.getPatients().size());
        } catch (Exception exception) {
            fail("could not execute against sandbox", exception);
        }
    }

    @DisplayName("With Year 2020 and filter set to false still return all (sandbox scenario)")
    @Test
    void findAllEvenWithYear() {

        Contract contract = new Contract();
        contract.setContractNumber("Z0001");
        contract.setContractName("Z0001");

        PatientContractCallable callable = new PatientContractCallable("Z0001", 1, 2020,
                bfdClient, true, TESTING_JOB_ID);

        try {
            ContractMapping results = callable.call();

            assertFalse(results.getPatients().isEmpty());
            assertEquals(1000, results.getPatients().size());
        } catch (Exception exception) {
            fail("could not execute against sandbox", exception);
        }
    }

    @DisplayName("With Year 2020 returns no data")
    @Test
    void filterAll() {

        Contract contract = new Contract();
        contract.setContractNumber("Z0001");
        contract.setContractName("Z0001");

        PatientContractCallable callable = new PatientContractCallable("Z0001", 1, 2020,
                bfdClient, false, TESTING_JOB_ID);

        try {
            ContractMapping results = callable.call();

            @SuppressWarnings("ConstantConditions")
            int discarded = (int) ReflectionTestUtils.getField(callable, "pastYear");
            assertEquals(1000, discarded);
            assertTrue(results.getPatients().isEmpty());
        } catch (Exception exception) {
            fail("could not execute against sandbox", exception);
        }
    }
}
