package gov.cms.ab2d.eventlogger;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Ab2dEnvironmentTest {

    @Test
    public void canProcessAllEnvironments() {
        Ab2dEnvironment env = Ab2dEnvironment.fromName("ab2d-dev");
        assertNotNull(env);
        assertEquals(Ab2dEnvironment.DEV, env);

        env = Ab2dEnvironment.fromName("ab2d-sbx-sandbox");
        assertNotNull(env);
        assertEquals(Ab2dEnvironment.SANDBOX, env);

        env = Ab2dEnvironment.fromName("ab2d-east-impl");
        assertNotNull(env);
        assertEquals(Ab2dEnvironment.IMPL, env);

        env = Ab2dEnvironment.fromName("ab2d-east-prod-test");
        assertNotNull(env);
        assertEquals(Ab2dEnvironment.PRODUCTION_VALIDATION, env);

        env = Ab2dEnvironment.fromName("ab2d-east-prod");
        assertNotNull(env);
        assertEquals(Ab2dEnvironment.PRODUCTION, env);

    }
}
