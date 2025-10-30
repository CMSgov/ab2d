package gov.cms.ab2d.metrics;

import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.metrics.CloudwatchEventHandler.deriveSqsQueueName;
import static org.junit.jupiter.api.Assertions.*;

class CloudwatchEventHandlerTest {

    @Test
    void testDeriveSqsQueueName() {
        assertEquals("ab2d-dev-events", deriveSqsQueueName("https://sqs.us-east-1.amazonaws.com/123456789/ab2d-dev-events"));
    }
}
