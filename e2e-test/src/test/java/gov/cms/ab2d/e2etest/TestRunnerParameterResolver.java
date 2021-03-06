package gov.cms.ab2d.e2etest;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

// This causes the default to run as LOCAL when TestRunner is ran as a JUnit test, when it's ran from TestLauncher,
// a custom parameter can be supplied to the constructor
// Additionally an environment variable can be set which dictates which Environment
// the tests are being run in
public class TestRunnerParameterResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == Environment.class;
    }

    /**
     * Resolve the {@link Environment} that tests are running in using an environment variable
     * if present otherwise default to {@link Environment#LOCAL}
     * @throws ParameterResolutionException inherited but should not be thrown
     */
    @Override
    public Environment resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) throws ParameterResolutionException {

        String environment = System.getenv("E2E_ENVIRONMENT");

        if (StringUtils.isBlank(environment)) {
            return Environment.LOCAL;
        }

        return Environment.valueOf(environment);
    }
}
