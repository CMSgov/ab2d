package gov.cms.ab2d.contracts.quartz;

import gov.cms.ab2d.contracts.service.AttestationUpdaterService;
import gov.cms.ab2d.contracts.service.FeatureEngagement;
import gov.cms.ab2d.properties.client.PropertiesClient;
import gov.cms.ab2d.properties.client.PropertiesClientImpl;
import gov.cms.ab2d.properties.client.PropertyNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;


@Slf4j
@DisallowConcurrentExecution
public class HPMSIngestJob extends QuartzJobBean {
    public static final String HPMS_INGESTION_ENGAGEMENT = "hpms.ingest.engaged";
    private final AttestationUpdaterService aus;

    private final PropertiesClient propertiesClient;

    public HPMSIngestJob(AttestationUpdaterService aus,
                         @Value("${property.service.url}") String propertyServiceUrl) {
        this.aus = aus;
        propertiesClient = new PropertiesClientImpl(propertyServiceUrl);
    }


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
        try {
            return FeatureEngagement.fromString(propertiesClient.getProperty(HPMS_INGESTION_ENGAGEMENT).getValue());
        }
        catch (PropertyNotFoundException e){
            return FeatureEngagement.IN_GEAR;
        }
    }
}
