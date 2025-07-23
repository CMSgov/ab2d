package gov.cms.ab2d.testutils;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ContextTest {

    Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    @Test
    void testContext() {
        TestContext testContext = new TestContext();

        assertDoesNotThrow(() -> {
            log.info(testContext.getAwsRequestId());
            log.info(testContext.getLogGroupName());
            log.info(testContext.getLogStreamName());
            log.info(testContext.getFunctionName());
            log.info(testContext.getFunctionVersion());
            log.info(testContext.getInvokedFunctionArn());
            log.info(String.valueOf(testContext.getIdentity()));
            log.info(String.valueOf(testContext.getClientContext()));
            log.info(String.valueOf(testContext.getRemainingTimeInMillis()));
            log.info(String.valueOf(testContext.getMemoryLimitInMB()));
            log.info(String.valueOf(testContext.getLogger()));

        });
    }
}
