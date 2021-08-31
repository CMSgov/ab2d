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

    private final Map<String, ProgressTracker>  progressTrackerMap = new HashMap<>(89);

    public JobProgressServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public void addMeasure(String jobId, JobMeasure measure, long value) {
        ProgressTracker progressTracker = progressTrackerMap.computeIfAbsent(jobId, (k) ->
                                                        ProgressTracker.builder().jobUuid(jobId).build());
        measure.update(progressTracker, value);

        // update the progress in the DB & logs periodically
        trackProgress(progressTracker);
    }

    @Override
    public ProgressTracker getStatus(String jobId) {
        return progressTrackerMap.get(jobId);
    }

    /**
     * Update the database or log with the % complete on the job periodically
     *
     * @param progressTracker - the progress tracker
     */

    private void trackProgress(ProgressTracker progressTracker) {
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

}
