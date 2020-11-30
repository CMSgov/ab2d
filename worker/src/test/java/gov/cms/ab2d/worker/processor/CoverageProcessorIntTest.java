package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Coverage;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class CoverageProcessorIntTest {

    @Container
    private static final PostgreSQLContainer postgres = new AB2DPostgresqlContainer();

    @Autowired
    private ContractRepository contractRepo;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    @Qualifier(value = "patientCoverageThreadPool")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private CoverageProcessor processor;

    @AfterEach
    void after() {
        dataSetup.deleteCoverage();
        coverageSearchEventRepo.deleteAll();
        coverageSearchRepo.deleteAll();
        coveragePeriodRepo.deleteAll();
    }

    @Disabled("Performance test meant to run against the sandbox, do not run as part of CI.")
    @Test
    void integrationTest() {


        Contract contract = contractRepo.findContractByContractNumber("Z0010").get();
        CoveragePeriod january = dataSetup.createCoveragePeriod(contract, 1, 2000);
        CoveragePeriod february = dataSetup.createCoveragePeriod(contract, 2, 2000);
        CoveragePeriod march = dataSetup.createCoveragePeriod(contract, 3, 2000);
        CoveragePeriod april = dataSetup.createCoveragePeriod(contract, 4, 2000);

        processor.queueCoveragePeriod(january, false);
        processor.queueCoveragePeriod(february, false);
        processor.queueCoveragePeriod(march, false);
        processor.queueCoveragePeriod(april, false);

        assertEquals(4, coverageSearchRepo.count());

        sleep(90);

        while (taskExecutor.getActiveCount() > 0) {
            sleep(2);
        }

        assertEquals(40000, dataSetup.countCoverage());
        assertEquals(4, dataSetup.findCoverage()
                .stream().map(Coverage::getPeriodId)
                .collect(toSet()).size());
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ie) {

        }
    }
}
