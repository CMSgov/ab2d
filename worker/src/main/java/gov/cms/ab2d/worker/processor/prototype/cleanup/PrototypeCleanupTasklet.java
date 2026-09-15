package gov.cms.ab2d.worker.processor.prototype.cleanup;

import gov.cms.ab2d.worker.processor.prototype.PrototypeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * One pass of cleanup. Deletes old rows from batch metadata tables, piece by piece.
 * TODO: May be a candidate for metrics
 */
@Slf4j
@Component
public class PrototypeCleanupTasklet implements Tasklet {

    private final PrototypeCleanupRepository repository;
    private final PrototypeProperties props;

    public PrototypeCleanupTasklet(PrototypeCleanupRepository repository, PrototypeProperties props) {
        this.repository = repository;
        this.props = props;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        long startedAt = System.currentTimeMillis();
        int retentionDays = props.getCleanupRetentionDays();
        int maxExecutions = props.getCleanupMaxExecutionsPerSweep();

        List<PrototypeCleanupRepository.EligibleExecution> expired =
                repository.eligibleExecutions(retentionDays, maxExecutions);
        List<String> jobUuids = expired.stream()
                .map(PrototypeCleanupRepository.EligibleExecution::jobUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        long rows = repository.deleteExecutions(expired.stream()
                .map(PrototypeCleanupRepository.EligibleExecution::executionId)
                .toList());
        rows += repository.deleteLeases(jobUuids);
        rows += repository.deleteOrphanJobInstances();

        log.info("Metadata cleanup deleted {} execution(s) and {} metadata row(s) in {}ms ",
                expired.size(), rows, System.currentTimeMillis() - startedAt);

        contribution.incrementWriteCount(rows);
        return RepeatStatus.FINISHED;
    }
}
