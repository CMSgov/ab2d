package gov.cms.ab2d.worker.service;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.BeneficiaryAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.BfdClientAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

    @Value("${efs.mount}")
    private String efsMount;

    @Autowired
    private FhirContext fhirContext;

    private final JobRepository jobRepository;
    private final BeneficiaryAdapter beneficiaryAdapter;
    private final BfdClientAdapter  bfdClientAdapter;


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public void putJobInProgress(String jobId) {

        final Job job = jobRepository.findByJobUuid(jobId);
        Assert.notNull(job, String.format("Job %s not found", jobId));

        // validate status is SUBMITTED
        if (!SUBMITTED.equals(job.getStatus())) {
            final String errMsg = String.format("Job %s is not in %s status.", jobId, SUBMITTED);
            throw new IllegalArgumentException(errMsg);
        }

        job.setStatus(IN_PROGRESS);

        jobRepository.save(job);
    }

    @Override
    public Job processJob(final String jobId) {

        final Job job = jobRepository.findByJobUuid(jobId);
        log.info("Found job : {} - {} - {} ", job.getId(), job.getJobUuid(), job.getStatus());

        final Sponsor sponsor = job.getUser().getSponsor();

        final List<Contract> attestedContracts = sponsor.getAttestedContracts();
        log.info("number of attested contracts : {}", attestedContracts.size());

        final Path outputDir = createJobOutputDirectory(job.getJobUuid());
        for (Contract contract : attestedContracts) {
            log.info("contract : {} ", contract.getContractNumber());
            processContract(outputDir, contract);
        }

        return job;
    }

    private Path createJobOutputDirectory(String jobUuid) {
        final Path outputDir = Paths.get(efsMount, jobUuid);
        Path outputDirectory = null;
        try {
            outputDirectory = Files.createDirectories(outputDir);
        } catch (IOException e) {
            final String errMsg = "Could not create output directory : ";
            log.error("{} : {}", errMsg, outputDir.toAbsolutePath());
            throw new RuntimeException(errMsg + outputDir.getFileName(), e);
        }
        return outputDirectory;
    }

    private void processContract(final Path outputDir, Contract contract) {
        final var ndJsonFile = createContractFile(outputDir, contract);

        final var patientsByContract = beneficiaryAdapter.getPatientsByContract(contract.getContractNumber());

        final var futureResourcesHandles =  new ArrayList<Future<List<Resource>>>();

        int counter = 0;
        for (var patient : patientsByContract.getPatients()) {
            final var resources = bfdClientAdapter.getEobBundleResources(patient.getPatientId());
            futureResourcesHandles.add(resources);

            ++counter;
            if (counter % 2 == 0) {
                processResources(futureResourcesHandles, ndJsonFile);
            }
        }

        processResources(futureResourcesHandles, ndJsonFile);
    }

    private Path createContractFile(final Path outputDir, Contract contract) {
        final String filename = contract.getContractNumber() + NDJSON_EXTENSION;
        final Path ndJsonPath = Path.of(outputDir.toString(), filename);
        Path outputContractNdJsonFile = null;
        try {
            outputContractNdJsonFile = Files.createFile(ndJsonPath);
            log.info("file: {} ", outputContractNdJsonFile.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputContractNdJsonFile;
    }

    private void processResources(List<Future<List<Resource>>> futureHandles, Path outputFile) {
        log.info("inside processResources() ...  waits for futures and writes to file");

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

                Files.write(outputFile, byteArrayOutputStream.toByteArray(), StandardOpenOption.APPEND);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            log.info("finished writing [{}] resources", resourceCount);
            iterator.remove();

        }

    }




    @Override
    public void completeJob(Job job) {
        job.setStatus(SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setExpiresAt(job.getCreatedAt().plusDays(1));

        jobRepository.save(job);
        log.info("Job: [{}] is DONE", job.getId());
    }




}
