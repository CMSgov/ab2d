package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.worker.processor.SerializedEobs;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;

import java.util.List;

/**
 * Creates two FlatFile writers that handle their own output files. The dataWriter writes the EOBs to ndjson, and
 * the errorWriter writes error lines to the _error file. Each writer can ONLY handle 1 file. Since we need two
 * output files, we need two writers. The composite writer keeps both synced.
 */
public class NdjsonCompositeWriter implements ItemStreamWriter<SerializedEobs> {

    private final FlatFileItemWriter<String> dataWriter;
    private final FlatFileItemWriter<String> errorWriter;

    public NdjsonCompositeWriter(FlatFileItemWriter<String> dataWriter, FlatFileItemWriter<String> errorWriter) {
        this.dataWriter = dataWriter;
        this.errorWriter = errorWriter;
    }

    @Override
    public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        dataWriter.open(executionContext);
        errorWriter.open(executionContext);
    }

    @Override
    public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        dataWriter.update(executionContext);
        errorWriter.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            dataWriter.close();
        } finally {
            errorWriter.close();
        }
    }

    @Override
    public void write(@NonNull Chunk<? extends SerializedEobs> chunk) throws Exception {
        List<String> dataLines = chunk.getItems().stream().flatMap(item -> item.dataLines().stream()).toList();
        List<String> errorLines = chunk.getItems().stream().flatMap(item -> item.errorLines().stream()).toList();
        if (!dataLines.isEmpty()) {
            dataWriter.write(new Chunk<>(dataLines));
        }
        if (!errorLines.isEmpty()) {
            errorWriter.write(new Chunk<>(errorLines));
        }
    }
}
