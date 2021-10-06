package gov.cms.ab2d.worker.quartz;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class CoverageCheckQuartzConfig {

    private final String schedule;

    public CoverageCheckQuartzConfig(@Value("${coverage.verify.schedule}") String schedule) {
        this.schedule = schedule;
    }

    @Qualifier("coverage_verifier")
    @Bean
    JobDetail coverageVerifierJobDetail() {
        return JobBuilder.newJob(CoverageCheckQuartzJob.class)
                .withIdentity("coverage_verifier")
                .storeDurably()
                .build();
    }

    @Bean
    Trigger coveragePeriodJobPeriodicTrigger(@Qualifier("coverage_verifier") JobDetail coverageCheckJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(coverageCheckJobDetail)
                .withIdentity("coverage_verifier_trigger_periodic")
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                .build();
    }
}
