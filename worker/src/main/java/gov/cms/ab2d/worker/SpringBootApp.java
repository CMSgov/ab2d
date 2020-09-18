package gov.cms.ab2d.worker;

import gov.cms.ab2d.worker.bfdhealthcheck.BFDHealthCheckQuartzSetup;
import gov.cms.ab2d.worker.stuckjob.StuckJobQuartzSetup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = {
        "gov.cms.ab2d.common",
        "gov.cms.ab2d.worker",
        "gov.cms.ab2d.bfd.client",
        "gov.cms.ab2d.audit",
        "gov.cms.ab2d.optout",
        "gov.cms.ab2d.eventlogger"
})
@EntityScan(basePackages = {"gov.cms.ab2d.common.model"})
@EnableJpaRepositories("gov.cms.ab2d.common.repository")
@EnableRetry
@PropertySource("classpath:application.common.properties")
/* Remove Quartz job for OptOut. If we want it back, add OptOutQuartzSetup.class below */
@Import({StuckJobQuartzSetup.class, BFDHealthCheckQuartzSetup.class})
public class SpringBootApp {

    // Why do you have to know every month?

    // 1000
    // 999 - january of a new patient
    // 1000 - february of a new patient

    // SELECT year, month, beneficiary FROM Coverage INNER JOIN CoveragePeriod
    // WHERE coveragePeriodId IN(list pertaining to date range and contract)
    // ORDER BY year, month
    // GROUP BY beneficiaryId

    // SELECT DISTINCT COUNT(beneficiaryId)
    // FROM coverage
    // WHERE coveragePeriodId IN(list pertaining to date range and contract)

    // SELECT month,year,beneficiaryId
    // FROM Coverage INNER JOIN CoveragePeriod
    // WHERE beneficiaryId IN (........)

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}
