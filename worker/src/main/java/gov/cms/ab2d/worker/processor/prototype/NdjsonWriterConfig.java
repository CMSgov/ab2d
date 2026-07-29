package gov.cms.ab2d.worker.processor.prototype;

import ca.uhn.fhir.parser.IParser;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.config.SearchConfig;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines the output writer using Spring Batch's FlatFileWriter
 */
@Configuration
public class NdjsonWriterConfig {

    @Bean
    @StepScope
    public FlatFileItemWriter<List<IBaseResource>> ndjsonItemWriter(
            JobRepository jobRepository,
            SearchConfig searchConfig,
            @Value("#{jobParameters['jobUuid']}") String jobUuid,
            @Value("#{jobParameters['fenceToken']}") long fenceToken,
            @Value("#{stepExecutionContext['contractNumber']}") String contract,
            @Value("#{stepExecutionContext['partitionIndex']}") int partitionIndex) throws IOException {

        Job job = jobRepository.findByJobUuid(jobUuid);
        FhirVersion version = job.getFhirVersion();
        IParser parser = version.getJsonParser().setPrettyPrint(false);

        // files are named with the fenceToken so that each time it bumps, there must be a new file
        // no stale worker can modify another worker's file.
        Path outputFile = Path.of(searchConfig.getEfsMount(), jobUuid, searchConfig.getStreamingDir())
                .resolve(contract + "_partition" + partitionIndex + "_t" + fenceToken + ".ndjson");
        // FlatFileItemWriter does not create parent directories
        Files.createDirectories(outputFile.getParent());

        // the writer and reader are kept in line along the same fence token.
        // forceSync happens on every chunk flush and on close so we can actually guarantee safety
        // when soft-resuming. The file will always match what the cursor says.
        return new FlatFileItemWriterBuilder<List<IBaseResource>>()
                .name("ndjsonItemWriter.t" + fenceToken)
                .resource(new FileSystemResource(outputFile))
                .forceSync(true)
                .lineAggregator(eobs -> eobs.stream()
                        .map(parser::encodeResourceToString)
                        .collect(Collectors.joining("\n")))
                .build(); // fresh run overwrites, restart truncates to position
    }
}
