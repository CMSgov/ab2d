package gov.cms.ab2d.worker.processor.prototype;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The injector actually halts the JVM, so these tests spy the halt out and just check the decision:
 * is it off by default, does it only fire at the configured point, and is the point match forgiving.
 */
class CrashInjectorTest {

    @Test
    void offByDefaultSoNothingCrashes() {
        CrashInjector injector = spy(new CrashInjector("process", 0));
        doNothing().when(injector).halt(anyString());

        injector.maybeCrash("process");

        verify(injector, never()).halt(anyString());
    }

    @Test
    void crashesOnlyAtTheConfiguredPoint() {
        CrashInjector injector = spy(new CrashInjector("write", 1.0));
        doNothing().when(injector).halt(anyString());

        injector.maybeCrash("read");
        injector.maybeCrash("process");
        verify(injector, never()).halt(anyString());

        injector.maybeCrash("write");
        verify(injector, times(1)).halt("write");
    }

    @Test
    void pointMatchIsCaseInsensitive() {
        CrashInjector injector = spy(new CrashInjector("ASSEMBLE", 1.0));
        doNothing().when(injector).halt(anyString());

        injector.maybeCrash("assemble");

        verify(injector, times(1)).halt("assemble");
    }
}
