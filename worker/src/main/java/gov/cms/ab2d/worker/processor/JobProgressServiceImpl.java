package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JobProgressServiceImpl implements JobProgressService {

    @Value("${report.progress.db.frequency:100}")
    private int reportProgressDbFrequency;

    @Value("${report.progress.log.frequency:100}")
    private int reportProgressLogFrequency;

    private final JobRepository jobRepository;

    private final Map<String, ProgressTracker> progressTrackerMap = new HashMap<>(89);

    public JobProgressServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public void addMeasure(String jobUuid, JobMeasure measure, long value) {
        ProgressTracker progressTracker = getOrCreateTracker(jobUuid);

        measure.update(progressTracker, value);

        updateProgress(progressTracker);
    }

    @Override
    public ProgressTracker getStatus(String jobUuid) {
        return progressTrackerMap.get(jobUuid);
    }

    /**
     * Update the database or log with the % complete on the job periodically
     *
     * @param progressTracker progressTracker
     */

    private void updateProgress(ProgressTracker progressTracker) {
        if (progressTracker.isTimeToUpdateDatabase(reportProgressDbFrequency)) {
            final int percentageCompleted = progressTracker.getPercentageCompleted();

            if (percentageCompleted > progressTracker.getLastUpdatedPercentage()) {
                jobRepository.updatePercentageCompleted(progressTracker.getJobUuid(), percentageCompleted);
                progressTracker.setLastUpdatedPercentage(percentageCompleted);
            }
        }

        var processedCount = progressTracker.getPatientRequestProcessedCount();
        if (progressTracker.isTimeToLog(reportProgressLogFrequency)) {
            progressTracker.setLastLogUpdateCount(processedCount);

            var totalCount = progressTracker.getTotalCount();
            var percentageCompleted = progressTracker.getPercentageCompleted();
            log.info("[{}/{}] records processed = [{}% completed]", processedCount, totalCount, percentageCompleted);
        }
    }

    private ProgressTracker getOrCreateTracker(String jobUuid) {
        return progressTrackerMap.computeIfAbsent(jobUuid, (k) ->
                ProgressTracker.builder().jobUuid(jobUuid).build());
    }
}
