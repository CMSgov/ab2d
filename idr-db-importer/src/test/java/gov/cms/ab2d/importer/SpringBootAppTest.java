package gov.cms.ab2d.importer;

import gov.cms.ab2d.common.properties.PropertiesService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.YearMonth;
import java.util.List;

import static gov.cms.ab2d.importer.SpringBootApp.STATUS_IN_PROGRESS;
import static gov.cms.ab2d.importer.SpringBootApp.STATUS_NOT_IN_PROGRESS;
import static gov.cms.ab2d.common.util.PropertyConstants.V3_IDR_IMPORTER_STATUS;
import static org.mockito.Mockito.*;

class SpringBootAppTest {

    @Test
    void run_exitsZero_onSuccess() throws Exception {
        CoverageV3S3Importer importer = mock(CoverageV3S3Importer.class);
        PropertiesService propertiesService = mock(PropertiesService.class);
        when(propertiesService.updateProperty(any(), any())).thenReturn(true);

        SpringBootApp app = spy(new SpringBootApp(importer, mock(CoverageV3Backfiller.class), propertiesService));
        doNothing().when(app).exit(anyInt());

        ApplicationArguments args = new DefaultApplicationArguments();
        app.run(args);

        verify(importer, times(1)).runOnce();
        verify(propertiesService).updateProperty(V3_IDR_IMPORTER_STATUS, STATUS_IN_PROGRESS);
        verify(propertiesService).updateProperty(V3_IDR_IMPORTER_STATUS, STATUS_NOT_IN_PROGRESS);
        verify(app).exit(0);
    }

    @Test
    void run_exitsOne_onFailure() throws Exception {
        CoverageV3S3Importer importer = mock(CoverageV3S3Importer.class);
        PropertiesService propertiesService = mock(PropertiesService.class);
        when(propertiesService.updateProperty(any(), any())).thenReturn(true);
        doThrow(new RuntimeException("exception")).when(importer).runOnce();

        SpringBootApp app = spy(new SpringBootApp(importer, mock(CoverageV3Backfiller.class), propertiesService));
        doNothing().when(app).exit(anyInt());

        ApplicationArguments args = new DefaultApplicationArguments();
        app.run(args);

        verify(importer, times(1)).runOnce();
        verify(propertiesService).updateProperty(V3_IDR_IMPORTER_STATUS, STATUS_IN_PROGRESS);
        verify(propertiesService).updateProperty(V3_IDR_IMPORTER_STATUS, STATUS_NOT_IN_PROGRESS);
        verify(app).exit(1);
    }

    @Test
    void run_backfillMode_runsBackfillInsteadOfDailyImport() throws Exception {
        CoverageV3S3Importer importer = mock(CoverageV3S3Importer.class);
        CoverageV3Backfiller backfiller = mock(CoverageV3Backfiller.class);
        PropertiesService propertiesService = mock(PropertiesService.class);

        SpringBootApp app = spy(new SpringBootApp(importer, backfiller, propertiesService));
        ReflectionTestUtils.setField(app, "backfillMonth", "2020-06");
        ReflectionTestUtils.setField(app, "backfillContracts", "s4802, S5601");
        doNothing().when(app).exit(anyInt());

        app.run(new DefaultApplicationArguments());

        verify(backfiller).run(new BackfillRequest(YearMonth.of(2020, 6), List.of("S4802", "S5601")));
        verify(importer, never()).runOnce();
        verifyNoInteractions(propertiesService);
        verify(app).exit(0);
    }

    @Test
    void run_backfillMode_exitsOne_onInvalidRequest() throws Exception {
        CoverageV3S3Importer importer = mock(CoverageV3S3Importer.class);
        CoverageV3Backfiller backfiller = mock(CoverageV3Backfiller.class);
        PropertiesService propertiesService = mock(PropertiesService.class);

        SpringBootApp app = spy(new SpringBootApp(importer, backfiller, propertiesService));
        ReflectionTestUtils.setField(app, "backfillMonth", "2020-06");
        ReflectionTestUtils.setField(app, "backfillContracts", "");
        doNothing().when(app).exit(anyInt());

        app.run(new DefaultApplicationArguments());

        verify(backfiller, never()).run(any());
        verify(importer, never()).runOnce();
        verify(app).exit(1);
    }

    @Test
    void run_backfillMode_exitsOne_onFailure() throws Exception {
        CoverageV3S3Importer importer = mock(CoverageV3S3Importer.class);
        CoverageV3Backfiller backfiller = mock(CoverageV3Backfiller.class);
        PropertiesService propertiesService = mock(PropertiesService.class);
        doThrow(new IllegalStateException("failed")).when(backfiller).run(any());

        SpringBootApp app = spy(new SpringBootApp(importer, backfiller, propertiesService));
        ReflectionTestUtils.setField(app, "backfillMonth", "2020-06");
        ReflectionTestUtils.setField(app, "backfillContracts", "S4802");
        doNothing().when(app).exit(anyInt());

        app.run(new DefaultApplicationArguments());

        verify(app).exit(1);
    }
}
