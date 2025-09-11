package gov.cms.ab2d.snsclient.clients;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.snsclient.clients.SNSClientImpl.getTopicPrefix;
import static org.junit.jupiter.api.Assertions.*;

class SNSClientImplTest {

    @Test
    void testTopicPrefix() {
        assertEquals("ab2d-dev", getTopicPrefix(Ab2dEnvironment.DEV));
        assertEquals("ab2d-test",getTopicPrefix(Ab2dEnvironment.IMPL));
        assertEquals("ab2d-sandbox", getTopicPrefix(Ab2dEnvironment.SANDBOX));
        assertEquals("ab2d-prod", getTopicPrefix(Ab2dEnvironment.PRODUCTION));

    }
}
