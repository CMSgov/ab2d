package gov.cms.ab2d.worker.processor.coverage.v3;

import gov.cms.ab2d.coverage.service.CoverageV3Service;
import gov.cms.ab2d.job.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CoverageV3Test {

	private final JobRepository jobRepository;
	private final CoverageV3Service coverageV3Service;

	public CoverageV3Test(JobRepository jobRepository, CoverageV3Service coverageV3Service) {
		this.jobRepository = jobRepository;
		this.coverageV3Service = coverageV3Service;
	}

	@Scheduled(fixedRate = 12, timeUnit = TimeUnit.HOURS)
	void copyFromStagingTable() {
		coverageV3Service.copyFromHistoricalStagingTables();
	}
}
