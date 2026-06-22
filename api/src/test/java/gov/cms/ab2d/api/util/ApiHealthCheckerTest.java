package gov.cms.ab2d.api.util;

import com.timgroup.statsd.StatsDClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiHealthCheckerTest {

    @Mock
    private StatsDClient statsDClient;

    @Test
    void checkHealthTest() {
        ApiHealthChecker checker = new ApiHealthChecker("ab2d-east-impl", statsDClient);

        checker.checkHealth();

        verify(statsDClient).gauge(
                ApiHealthChecker.HEALTH_GAUGE, 1, "environment:" + Ab2dEnvironment.IMPL.getName());
    }
}
