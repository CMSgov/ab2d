package gov.cms.ab2d.worker.processor.prototype;

import ca.uhn.fhir.parser.IParser;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.config.SearchConfig;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Writes each item's resources as lines to the partition's output file.
 * This will likely need (significant) changes for remote partitioning
 */
@Slf4j
@Component
@StepScope
public class NdjsonItemWriter implements ItemStreamWriter<List<IBaseResource>> {

    static final String POSITION_KEY = "ndjson.writer.position";

    private final FhirVersion version;
    private final Path outputFile;

    private FileChannel channel;
    private Writer writer;

    public NdjsonItemWriter(
            JobRepository jobRepository,
            SearchConfig searchConfig,
            @Value("#{jobParameters['jobUuid']}") String jobUuid,
            @Value("#{stepExecutionContext['contractNumber']}") String contract,
            @Value("#{stepExecutionContext['partitionIndex']}") int partitionIndex
    ) {
        Job job = jobRepository.findByJobUuid(jobUuid);
        this.version = job.getFhirVersion();
        this.outputFile = Path.of(searchConfig.getEfsMount(), jobUuid, searchConfig.getFinishedDir())
                .resolve(contract + "_partition" + partitionIndex + ".ndjson");
    }

    @Override
    public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        try {
            Files.createDirectories(outputFile.getParent());
            channel = FileChannel.open(outputFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

            long committed = executionContext.containsKey(POSITION_KEY) ? executionContext.getLong(POSITION_KEY) : 0L;
            // Never seek past the end: if the file is shorter than the committed position it did not survive
            // the restart, and resuming would leave a gap. Clamp and warn instead of writing a corrupt file.
            long position = Math.min(committed, channel.size());
            if (position < committed) {
                log.warn("Output file {} is shorter ({} bytes) than the last committed position ({} bytes) - "
                        + "prior output was lost (is efs.mount persistent across restarts?); resumed file will be incomplete",
                        outputFile, channel.size(), committed);
            }
            // drop any rolled-back / partially written tail so the file matches the persisted reader cursor
            channel.truncate(position);
            channel.position(position);
            if (position > 0) {
                log.info("Resuming writer for {} at byte {}", outputFile.getFileName(), position);
            }
            writer = new BufferedWriter(Channels.newWriter(channel, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ItemStreamException("Unable to open output file " + outputFile, e);
        }
    }

    @Override
    public void write(Chunk<? extends List<IBaseResource>> chunk) throws IOException {
        IParser parser = version.getJsonParser().setPrettyPrint(false);
        for (List<IBaseResource> eobs : chunk) {
            for (IBaseResource eob : eobs) {
                writer.write(parser.encodeResourceToString(eob));
                writer.write("\n");
            }
        }
        writer.flush();
    }

    @Override
    public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        try {
            writer.flush();
            // committed alongside the reader cursor at each chunk boundary, so the two stay in lockstep
            executionContext.putLong(POSITION_KEY, channel.position());
        } catch (IOException e) {
            throw new ItemStreamException("Unable to record writer position for " + outputFile, e);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new ItemStreamException("Unable to close output file " + outputFile, e);
            }
        }
    }

}
