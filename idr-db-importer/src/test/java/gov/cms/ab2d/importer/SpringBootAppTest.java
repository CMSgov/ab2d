package gov.cms.ab2d.importer;

import gov.cms.ab2d.common.properties.PropertiesService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

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

        SpringBootApp app = spy(new SpringBootApp(importer, propertiesService));
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

        SpringBootApp app = spy(new SpringBootApp(importer, propertiesService));
        doNothing().when(app).exit(anyInt());

        ApplicationArguments args = new DefaultApplicationArguments();
        app.run(args);

        verify(importer, times(1)).runOnce();
        verify(propertiesService).updateProperty(V3_IDR_IMPORTER_STATUS, STATUS_IN_PROGRESS);
        verify(propertiesService).updateProperty(V3_IDR_IMPORTER_STATUS, STATUS_NOT_IN_PROGRESS);
        verify(app).exit(1);
    }
}
