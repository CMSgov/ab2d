package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.worker.processor.JobMeasure;
import gov.cms.ab2d.worker.processor.SerializedEobs;
import gov.cms.ab2d.worker.service.JobChannelService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.SkipListener;

/**
 * Counts beneficiaries skipped for any reason as an error in the progress tracker.
 */
@Slf4j
public class PrototypeSkipCounter implements SkipListener<CoverageSummary, SerializedEobs> {

    private final JobChannelService channel;
    private final String jobUuid;

    public PrototypeSkipCounter(JobChannelService channel, String jobUuid) {
        this.channel = channel;
        this.jobUuid = jobUuid;
    }

    @Override
    public void onSkipInProcess(@NonNull CoverageSummary item, @NonNull Throwable t) {
        recordSkip(t);
    }

    @Override
    public void onSkipInRead(@NonNull Throwable t) {
        recordSkip(t);
    }

    @Override
    public void onSkipInWrite(@NonNull SerializedEobs item, @NonNull Throwable t) {
        recordSkip(t);
    }

    private void recordSkip(Throwable t) {
        log.warn("job {}: skipping a beneficiary after a persistent failure: {}", jobUuid, t.toString());
        channel.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUESTS_ERRORED, 1);
    }
}
