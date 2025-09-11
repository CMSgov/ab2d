package gov.cms.ab2d.contracts.quartz;

import gov.cms.ab2d.contracts.repository.PropertiesRepository;
import gov.cms.ab2d.contracts.service.AttestationUpdaterService;
import gov.cms.ab2d.contracts.service.FeatureEngagement;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@DisallowConcurrentExecution
public class HPMSIngestJob extends QuartzJobBean {
    public static final String HPMS_INGESTION_ENGAGEMENT = "hpms.ingest.engaged";
    private final AttestationUpdaterService attestationUpdaterService;
    private final PropertiesRepository propertiesRepository;

    public HPMSIngestJob(AttestationUpdaterService attestationUpdaterService, PropertiesRepository propertiesRepository) {
        this.attestationUpdaterService = attestationUpdaterService;
        this.propertiesRepository = propertiesRepository;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    protected void executeInternal(JobExecutionContext context) {
        // todo additionally filter hpms engagement by tracking the environment ab2d is running in.
        //      if the environment is not sandbox or prod do not poll hpms
        if (FeatureEngagement.IN_GEAR == getEngagement()) {
            attestationUpdaterService.pollOrganizations();
        }
        else {
            log.info("Skipping HPMS sync");
        }
    }

    public FeatureEngagement getEngagement() {
        try {
            val value = propertiesRepository.findByKey(HPMS_INGESTION_ENGAGEMENT);
            if (value.isPresent()) {
                return FeatureEngagement.fromString(value.get().getValue());
            }
        }
        catch (Exception e){
            log.error("Error retrieving '{}' property", HPMS_INGESTION_ENGAGEMENT, e);
        }

        return FeatureEngagement.IN_GEAR;
    }
}
