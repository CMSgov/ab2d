package gov.cms.ab2d.eventclient.config;


import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Ab2dEnvironmentTest {

    @Bean
    public Ab2dEnvironment getEnvironment() {
        return Ab2dEnvironment.fromName("local");
    }
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
