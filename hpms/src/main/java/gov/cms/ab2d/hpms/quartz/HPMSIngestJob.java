package gov.cms.ab2d.hpms.quartz;

import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.properties.service.PropertiesAPIService;
import gov.cms.ab2d.hpms.service.AttestationUpdaterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.scheduling.quartz.QuartzJobBean;

import static gov.cms.ab2d.common.util.PropertyConstants.HPMS_INGESTION_ENGAGEMENT;

@Slf4j
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class HPMSIngestJob extends QuartzJobBean {

    private final AttestationUpdaterService aus;
    private final PropertiesAPIService propertiesApiService;

    @SuppressWarnings("NullableProblems")
    @Override
    protected void executeInternal(JobExecutionContext context) {
        // todo additionally filter hpms engagement by tracking the environment ab2d is running in.
        //      if the environment is not sandbox or prod do not poll hpms
        if (FeatureEngagement.IN_GEAR == getEngagement()) {
            aus.pollOrganizations();
        }
    }

    public FeatureEngagement getEngagement() {
        return FeatureEngagement.fromString(propertiesApiService.getProperty(HPMS_INGESTION_ENGAGEMENT));
    }
}
