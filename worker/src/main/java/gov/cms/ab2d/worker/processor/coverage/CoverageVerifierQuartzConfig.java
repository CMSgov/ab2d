package gov.cms.ab2d.worker.processor.coverage;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class CoverageVerifierQuartzConfig {

    private final String schedule;

    public CoverageVerifierQuartzConfig(@Value("${coverage.verify.schedule}") String schedule) {
        this.schedule = schedule;
    }

    @Qualifier("coverage_verifier")
    @Bean
    JobDetail coverageVerifierJobDetail() {
        return JobBuilder.newJob(CoveragePeriodQuartzJob.class)
                .withIdentity("coverage_verifier")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger coveragePeriodJobPeriodicTrigger(@Qualifier("coverage_verifier") JobDetail coveragePeriodJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(coveragePeriodJobDetail)
                .withIdentity("coverage_verifier_trigger_periodic")
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                .build();
    }
}
