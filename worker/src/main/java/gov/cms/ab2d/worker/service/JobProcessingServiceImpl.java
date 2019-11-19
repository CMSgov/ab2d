package gov.cms.ab2d.worker.service;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.BeneficiaryAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.BfdClientAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static gov.cms.ab2d.common.model.JobStatus.IN_PROGRESS;
import static gov.cms.ab2d.common.model.JobStatus.SUBMITTED;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobProcessingServiceImpl implements JobProcessingService {
    private static final String NDJSON_EXTENSION = ".ndjson";
    private static final int GROUP_SIZE = 5;

    @Value("${efs.mount}")
    private String efsMount;

    private final FhirContext fhirContext;
    private final FileService fileService;
    private final JobRepository jobRepository;
    private final JobOutputRepository jobOutputRepository;
    private final BeneficiaryAdapter beneficiaryAdapter;
    private final BfdClientAdapter  bfdClientAdapter;


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public Job putJobInProgress(String jobId) {

        final Job job = jobRepository.findByJobUuid(jobId);
        Assert.notNull(job, String.format("Job %s not found", jobId));

        // validate status is SUBMITTED
        if (!SUBMITTED.equals(job.getStatus())) {
            final String errMsg = String.format("Job %s is not in %s status.", jobId, SUBMITTED);
            throw new IllegalArgumentException(errMsg);
        }

        job.setStatus(IN_PROGRESS);

        return jobRepository.save(job);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Job processJob(final String jobId) {

        final Job job = jobRepository.findByJobUuid(jobId);
        log.info("Found job : {} - {} - {} ", job.getId(), job.getJobUuid(), job.getStatus());

        final Sponsor sponsor = job.getUser().getSponsor();

        final List<Contract> attestedContracts = sponsor.getAttestedContracts();
        log.info("Job [{}] has [{}] attested contracts", job.getJobUuid(), attestedContracts.size());


        final Path outputDirPath = Paths.get(efsMount, job.getJobUuid());
        final Path outputDir = fileService.createDirectory(outputDirPath);
        for (Contract contract : attestedContracts) {
            log.info("Job [{}] - contract [{}] ", job.getJobUuid(), contract.getContractNumber());

            JobOutput jobOutput = null;
            try {
                jobOutput = processContract(outputDir, contract);
                jobOutput.setError(false);
            } catch (Exception e) {
                jobOutput.setError(true);
                log.error("error processing contract : {} ", contract.getContractNumber(), e);
            }

            job.addJobOutput(jobOutput);
            jobOutputRepository.save(jobOutput);
        }

        completeJob(job);
        return job;
    }


    private JobOutput processContract(final Path outputDir, Contract contract) {
        final String filename = contract.getContractNumber() + NDJSON_EXTENSION;
        final var ndJsonFile = fileService.createFile(outputDir, filename);

        final var patientsByContract = beneficiaryAdapter.getPatientsByContract(contract.getContractNumber());

        final var futureResourcesHandles =  new ArrayList<Future<List<Resource>>>();

        int counter = 0;
        for (var patient : patientsByContract.getPatients()) {
            final var resources = bfdClientAdapter.getEobBundleResources(patient.getPatientId());
            futureResourcesHandles.add(resources);

            ++counter;
            if (counter % GROUP_SIZE == 0) {
                processResources(futureResourcesHandles, ndJsonFile);
            }
        }

        processResources(futureResourcesHandles, ndJsonFile);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setFilePath(ndJsonFile.toFile().getName());
        jobOutput.setFhirResourceType("ExplanationOfBenefits");

        return jobOutput;
    }


    private void processResources(List<Future<List<Resource>>> futureHandles, Path outputFile) {

        final Iterator<Future<List<Resource>>> iterator = futureHandles.iterator();
        while (iterator.hasNext()) {
            final Future<List<Resource>> futureResources = iterator.next();

            List<Resource> resources = null;
            try {
                resources = futureResources.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

            var jsonParser = fhirContext.newJsonParser();
            int resourceCount = 0;

            try {
                var byteArrayOutputStream = new ByteArrayOutputStream();
                for (var resource : resources) {
                    ++resourceCount;
                    final String payload = jsonParser.encodeResourceToString(resource) + System.lineSeparator();
                    byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                fileService.appendToFile(outputFile, byteArrayOutputStream);

            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            log.info("finished writing [{}] resources", resourceCount);
            iterator.remove();

        }

    }


    private void completeJob(Job job) {
        job.setStatus(SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setExpiresAt(OffsetDateTime.now().plusDays(1));

        jobRepository.save(job);
        log.info("Job: [{}] is DONE", job.getId());
    }




}
