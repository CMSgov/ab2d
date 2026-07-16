package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Streams one partition's beneficiaries, paging the coverage table by patient_id
 */
@Slf4j
@Component
@StepScope
public class BeneficiaryItemReader implements ItemStreamReader<CoverageSummary> {
    static final String CURSOR_KEY = "beneficiary.reader.cursor";

    private final CoverageV3Service coverageV3Service;
    private final String contract;
    private final long startPatientId; // exclusive lower bound of this partition
    private final long endPatientId;   // inclusive upper bound of this partition
    private final int pageSize;

    private final Deque<CoverageSummary> buffer = new ArrayDeque<>(); // non-remote queue, for now
    private Long lastReadPatientId; // checkpoint
    private Long fetchCursor; // in-memory paging cursor
    private boolean exhausted;

    public BeneficiaryItemReader(
            CoverageV3Service coverageV3Service,
            @Value("#{stepExecutionContext['contractNumber']}") String contract,
            @Value("#{stepExecutionContext['startPatientId']}") long startPatientId,
            @Value("#{stepExecutionContext['endPatientId']}") long endPatientId,
            @Value("${eob.job.patient.queue.page.size}") int pageSize) {
        this.coverageV3Service = coverageV3Service;
        this.contract = contract;
        this.startPatientId = startPatientId;
        this.endPatientId = endPatientId;
        this.pageSize = pageSize;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        buffer.clear();
        exhausted = false;
        if (executionContext.containsKey(CURSOR_KEY)) {
            lastReadPatientId = executionContext.getLong(CURSOR_KEY);
            fetchCursor = lastReadPatientId;
            log.info("Resuming partition from patient {}", lastReadPatientId);
        } else {
            lastReadPatientId = null;
            fetchCursor = null;
            log.info("Starting partition from patient after {}", startPatientId);
        }
    }

    @Override
    public CoverageSummary read() {
        while (buffer.isEmpty() && !exhausted) {
            fetchNextPage();
        }
        if (buffer.isEmpty()) {
            return null;
        }
        CoverageSummary next = buffer.poll();
        lastReadPatientId = next.getIdentifiers().getPatientIdV3();
        return next;
    }

    private void fetchNextPage() {
        CoveragePagingResult result = coverageV3Service.pageCoverageByPatientRange(
                contract, startPatientId, endPatientId, Optional.ofNullable(fetchCursor), pageSize);
        buffer.addAll(result.getCoverageSummaries());

        if (result.getNextRequest().isPresent() && result.getNextRequest().get().getCursor().isPresent()) {
            fetchCursor = result.getNextRequest().get().getCursor().get();
        } else {
            exhausted = true;
        }
    }

    @Override
    public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        if (lastReadPatientId != null) {
            executionContext.putLong(CURSOR_KEY, lastReadPatientId);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        buffer.clear();
    }
}
