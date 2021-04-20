package gov.cms.ab2d.eventlogger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * List of environments AB2D can be deployed in.
 *
 * These environments can dictate logging behavior. In the local environment event logging is automatically disabled.
 */
public enum Ab2dEnvironment {
    LOCAL("local"),
    DEV("ab2d-dev"),
    IMPL("ab2d-east-impl"),
    SANDBOX("ab2d-sbx-sandbox"),
    PRODUCTION_VALIDATION("ab2d-east-prod-test"),
    PRODUCTION("ab2d-east-prod");

    public static final List<Ab2dEnvironment> PROD_LIST = Collections.singletonList(PRODUCTION);

    // Name typically expected in application.eventlogger.properties config
    // or passed in as env variable during startup
    private final String name;

    Ab2dEnvironment(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Ab2dEnvironment fromName(String name) {
        return Stream.of(Ab2dEnvironment.values())
                .filter(env -> env.getName().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(name + " is not a valid environment"));
    }
}
