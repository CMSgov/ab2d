package gov.cms.ab2d.worker.service.coveragesnapshot;

import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.snsclient.messages.AB2DServices;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;


@SpringBootTest
@Testcontainers
@Import(AB2DSQSMockConfig.class)
public class CoverageSnapshotTest {

    @SuppressWarnings("rawtypes")
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();

    @Autowired
    CoverageSnapshotService coverageSnapshotService;

    @MockBean
    private CoverageService coverageService;

    @Test
    void sendCoverageCounts() {
        Mockito.when(coverageService.countBeneficiariesForContracts(any()))
                .thenReturn(Arrays.asList(new CoverageCount("test", 1, 1, 1, 1, 1), new CoverageCount("test", 1, 1, 1, 1, 1)));
        coverageSnapshotService.sendCoverageCounts(AB2DServices.AB2D);
    }


}
