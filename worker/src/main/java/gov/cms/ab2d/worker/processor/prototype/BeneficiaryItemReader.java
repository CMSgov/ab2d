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

@Slf4j
@Component
@StepScope
public class BeneficiaryItemReader implements ItemStreamReader<CoverageSummary> {
    static final String CURSOR_KEY = "beneficiary.reader.cursor";

    private final CoverageV3Service coverageV3Service;
    private final String contract;
    private final long startRow;
    private final long endRow;
    private final int pageSize;

    private final Deque<CoverageSummary> buffer = new ArrayDeque<>(); // non-remote queue
    private Long lastReadRowNumber; // checkpoint
    private Long fetchCursor; // in-memory checkpoint
    private boolean exhausted;

    public BeneficiaryItemReader(
            CoverageV3Service coverageV3Service,
            @Value("#{stepExecutionContext['contractNumber']}") String contract,
            @Value("#{stepExecutionContext['startRow']}") long startRow,
            @Value("#{stepExecutionContext['endRow']}") long endRow,
            @Value("${eob.job.patient.queue.page.size}") int pageSize) {
        this.coverageV3Service = coverageV3Service;
        this.contract = contract;
        this.startRow = startRow;
        this.endRow = endRow;
        this.pageSize = pageSize;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        buffer.clear();
        exhausted = false;
        if(executionContext.containsKey(CURSOR_KEY)) {
            lastReadRowNumber = executionContext.getLong(CURSOR_KEY);
            fetchCursor = lastReadRowNumber;
            log.info("Resuming partition from row {}", lastReadRowNumber);
        } else {
            lastReadRowNumber = null;
            fetchCursor = null;
            log.info("Starting partition from row {}", startRow);
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
        lastReadRowNumber = next.getIdentifiers().getRowNumberV3();
        return next;
    }

    private void fetchNextPage() {
        CoveragePagingResult result = coverageV3Service.pageCoverageByRowRange(
                contract, startRow, endRow, Optional.ofNullable(fetchCursor), pageSize);
        buffer.addAll(result.getCoverageSummaries());

        if (result.getNextRequest().isPresent() && result.getNextRequest().get().getCursor().isPresent()){
            fetchCursor = result.getNextRequest().get().getCursor().get();
        } else {
            exhausted = true;
        }
    }

    @Override
    public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        if (lastReadRowNumber != null) {
            executionContext.putLong(CURSOR_KEY, lastReadRowNumber);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        buffer.clear();
    }
}
