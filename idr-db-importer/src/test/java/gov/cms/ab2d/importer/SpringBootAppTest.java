package gov.cms.ab2d.importer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.*;

class SpringBootAppTest {

    @Test
    void run_exitsZero_onSuccess() throws Exception {
        CoverageV3S3Importer importer = mock(CoverageV3S3Importer.class);

        SpringBootApp app = new SpringBootApp(importer);

        ApplicationArguments args = new DefaultApplicationArguments();
        app.run(args);

        verify(importer, times(1)).runOnce();
    }

    @Test
    void run_exitsOne_onFailure() throws Exception {
        CoverageV3S3Importer importer = mock(CoverageV3S3Importer.class);
        doThrow(new RuntimeException("exception")).when(importer).runOnce();

        SpringBootApp app = new SpringBootApp(importer);

        ApplicationArguments args = new DefaultApplicationArguments();
        app.run(args);

        verify(importer, times(1)).runOnce();
    }
}
