package gov.cms.ab2d.hpms.quartz;

import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import gov.cms.ab2d.hpms.service.AttestationUpdaterService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Method;

import static gov.cms.ab2d.common.service.FeatureEngagement.IN_GEAR;
import static gov.cms.ab2d.common.service.FeatureEngagement.NEUTRAL;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
class InjestMockTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @MockBean
    AttestationUpdaterService aus;

    @MockBean
    PropertiesService propertiesService;

    @Mock
    JobExecutionContext context;


    @Test
    void getEngagementIdle(){
        HPMSIngestJob hpmsIngestJob = new HPMSIngestJob(aus, propertiesService);
        Properties props = new Properties();
        props.setKey(Constants.HPMS_INGESTION_ENGAGEMENT);
        props.setValue("idle");
        when(propertiesService.getPropertiesByKey(Constants.HPMS_INGESTION_ENGAGEMENT)).thenReturn(props);
        Assertions.assertEquals(NEUTRAL, hpmsIngestJob.getEngagement());
    }

    @Test
    void getEngagementInGear(){
        HPMSIngestJob hpmsIngestJob = new HPMSIngestJob(aus, propertiesService);
        Properties props = new Properties();
        props.setKey(Constants.HPMS_INGESTION_ENGAGEMENT);
        props.setValue("notIdle");
        when(propertiesService.getPropertiesByKey(Constants.HPMS_INGESTION_ENGAGEMENT)).thenReturn(props);
        Assertions.assertEquals(IN_GEAR, hpmsIngestJob.getEngagement());
    }

    @Test
    void executeInternalIdle() throws NoSuchMethodException {
        HPMSIngestJob hpmsIngestJob = new HPMSIngestJob(aus, propertiesService);
        Properties props = new Properties();
        props.setKey(Constants.HPMS_INGESTION_ENGAGEMENT);
        props.setValue("idle");
        when(propertiesService.getPropertiesByKey(Constants.HPMS_INGESTION_ENGAGEMENT)).thenReturn(props);
        Method method = hpmsIngestJob.getClass().getDeclaredMethod("executeInternal", JobExecutionContext.class);
        method.setAccessible(true);
        Assertions.assertDoesNotThrow(() -> method.invoke(hpmsIngestJob, context));
    }

    @Test
    void executeInternalInGear() throws NoSuchMethodException {
        HPMSIngestJob hpmsIngestJob = new HPMSIngestJob(aus, propertiesService);
        Properties props = new Properties();
        props.setKey(Constants.HPMS_INGESTION_ENGAGEMENT);
        props.setValue("notIdle");
        when(propertiesService.getPropertiesByKey(Constants.HPMS_INGESTION_ENGAGEMENT)).thenReturn(props);
        Method method = hpmsIngestJob.getClass().getDeclaredMethod("executeInternal", JobExecutionContext.class);
        method.setAccessible(true);
        Assertions.assertDoesNotThrow(() -> method.invoke(hpmsIngestJob, context));
    }
}
