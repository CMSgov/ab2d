package gov.cms.ab2d.worker.processor.prototype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** halt() is stubbed out so we can check the arm/fire decision without killing the test JVM. */
class CrashInjectorTest {

    @Test
    void offByDefaultSoNothingCrashes() {
        CrashInjector injector = spy(new CrashInjector("process", 0, "test"));
        doNothing().when(injector).halt(any());
        injector.maybeCrash(CrashPoint.PROCESS);
        verify(injector, never()).halt(any());
    }

    @Test
    void crashesOnlyAtTheConfiguredPoint() {
        CrashInjector injector = spy(new CrashInjector("write", 1.0, "test"));
        doNothing().when(injector).halt(any());
        injector.maybeCrash(CrashPoint.READ);
        injector.maybeCrash(CrashPoint.PROCESS);
        verify(injector, never()).halt(any());
        injector.maybeCrash(CrashPoint.WRITE);
        verify(injector, times(1)).halt(CrashPoint.WRITE);
    }

    @Test
    void configValueIsCaseInsensitive() {
        CrashInjector injector = spy(new CrashInjector("ASSEMBLE", 1.0, "test"));
        doNothing().when(injector).halt(any());
        injector.maybeCrash(CrashPoint.ASSEMBLE);
        verify(injector, times(1)).halt(CrashPoint.ASSEMBLE);
    }

    @Test
    void unknownConfigValueDoesNotArm() {
        CrashInjector injector = spy(new CrashInjector("banana", 1.0, "test"));
        doNothing().when(injector).halt(any());
        injector.maybeCrash(CrashPoint.WRITE);
        verify(injector, never()).halt(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab2d-east-prod", "ab2d-sbx-sandbox", "an-env-we-dont-know"})
    void refusesToArmOutsideDevTestLocal(String executionEnv) {
        CrashInjector injector = spy(new CrashInjector("write", 1.0, executionEnv));
        doNothing().when(injector).halt(any());
        injector.maybeCrash(CrashPoint.WRITE);
        verify(injector, never()).halt(any());
    }
}
