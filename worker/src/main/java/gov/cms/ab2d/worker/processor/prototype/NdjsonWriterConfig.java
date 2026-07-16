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
            @Value("#{stepExecutionContext['contractNumber']}") String contract,
            @Value("#{stepExecutionContext['partitionIndex']}") int partitionIndex) throws IOException {

        Job job = jobRepository.findByJobUuid(jobUuid);
        FhirVersion version = job.getFhirVersion();
        IParser parser = version.getJsonParser().setPrettyPrint(false);

        Path outputFile = Path.of(searchConfig.getEfsMount(), jobUuid, searchConfig.getFinishedDir())
                .resolve(contract + "_partition" + partitionIndex + ".ndjson");
        // FlatFileItemWriter does not create parent directories
        Files.createDirectories(outputFile.getParent());

        return new FlatFileItemWriterBuilder<List<IBaseResource>>()
                .name("ndjsonItemWriter")
                .resource(new FileSystemResource(outputFile))
                .lineAggregator(eobs -> eobs.stream()
                        .map(parser::encodeResourceToString)
                        .collect(Collectors.joining("\n")))
                .build(); // fresh run overwrites, restart truncates to position
    }
}
