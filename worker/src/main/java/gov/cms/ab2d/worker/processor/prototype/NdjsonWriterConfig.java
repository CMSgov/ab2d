package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.worker.config.SearchConfig;
import gov.cms.ab2d.worker.processor.SerializedEobs;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Defines the writer for a worker step. Consists of a {@link NdjsonCompositeWriter} with two
 * flat file writers. Lines are already serialized, so this just passes them along.
 */
@Configuration
public class NdjsonWriterConfig {

    @Bean
    @StepScope
    public ItemStreamWriter<SerializedEobs> ndjsonItemWriter(
            SearchConfig searchConfig,
            @Value("#{jobParameters['jobUuid']}") String jobUuid,
            @Value("#{jobParameters['fenceToken']}") long fenceToken,
            @Value("#{stepExecutionContext['contractNumber']}") String contract,
            @Value("#{stepExecutionContext['partitionIndex']}") int partitionIndex) throws IOException {


        Path streamingDir = Path.of(searchConfig.getEfsMount(), jobUuid, searchConfig.getStreamingDir());
        // FlatFileItemWriter does not create parent directories
        Files.createDirectories(streamingDir);

        // files are named with the fenceToken so that each time it bumps there must be a new file. This keeps
        // restart safe. Stale/zombie workers cannot interact with the new file.
        String base = contract + "_partition" + partitionIndex + "_t" + fenceToken;
        FlatFileItemWriter<String> dataWriter = lineWriter(
                streamingDir.resolve(base + ".ndjson"), "ndjsonDataWriter.p" + partitionIndex + ".t" + fenceToken);
        FlatFileItemWriter<String> errorWriter = lineWriter(
                streamingDir.resolve(base + "_error.ndjson"), "ndjsonErrorWriter.p" + partitionIndex + ".t" + fenceToken);
        return new NdjsonCompositeWriter(dataWriter, errorWriter);
    }

    /**
     * Simple line writer. forceSync true ensures that the file matches the saved offset, which
     * allows for soft-resume.
     */
    private static FlatFileItemWriter<String> lineWriter(Path outputFile, String name) {
        return new FlatFileItemWriterBuilder<String>()
                .name(name)
                .resource(new FileSystemResource(outputFile))
                .forceSync(true)
                .lineAggregator(line -> line)
                .build();
    }
}
