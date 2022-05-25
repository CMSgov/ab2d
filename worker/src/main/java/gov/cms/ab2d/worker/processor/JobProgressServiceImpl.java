package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.job.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JobProgressServiceImpl implements JobProgressService, JobProgressUpdateService {

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
    public void initJob(String jobUuid) {
        progressTrackerMap.put(jobUuid, ProgressTracker.builder().jobUuid(jobUuid).build());
    }

    @Override
    public boolean hasJob(String jobUuid) {
        return progressTrackerMap.containsKey(jobUuid);
    }

    @Override
    public boolean addMeasure(String jobId, JobMeasure measure, long value) {
        ProgressTracker progressTracker = progressTrackerMap.get(jobId);
        // Most likely being tracked by another instance in the cluster.  Less likely a very stale message and the
        // progressTracker has been aged out.
        if (progressTracker == null) {
            return false;
        }
        measure.update(progressTracker, value);

        // update the progress in the DB & logs periodically
        trackProgress(progressTracker);
        return true;
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
