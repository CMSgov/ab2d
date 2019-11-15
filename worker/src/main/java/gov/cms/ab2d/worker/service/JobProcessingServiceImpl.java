package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.BeneficiaryAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.BfdClientAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.BfdClientAdapterImpl.EobBundleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        log.info("Found Sponsor : {} - {} - {} ", sponsor.getId(), sponsor.getLegalName(), sponsor.getOrgName());

        final List<Contract> attestedContracts = sponsor.getAttestedContracts();
        log.info("number of attested contracts : {}", attestedContracts.size());

        final Path outputDir = createJobOutputDirectory(job.getJobUuid());
        for (Contract contract : attestedContracts) {
            log.info("contract : {} ", contract.getContractName());
            processContract(outputDir, contract);
        }

        return job;
    }

    private Path createJobOutputDirectory(String jobUuid) {
        final Path outputDir = Paths.get(efsMount, jobUuid);
        try {
            final Path outputDirectory = Files.createDirectories(outputDir);
            log.info("OutputDir: {} ", outputDirectory.toAbsolutePath());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return outputDir;
    }


    private void processContract(final Path outputDir, Contract contract) {
        final var ndJsonFile = createContractFile(outputDir, contract);

        final var patientsByContract = beneficiaryAdapter.getPatientsByContract(contract.getContractNumber());

        final var eobBundles =  new ArrayList<EobBundleDTO>();

        for (var patient : patientsByContract.getPatients()) {
            eobBundles.add(bfdClientAdapter.getEobBundle(patient.getPatientId()));
            parseEobBundles(eobBundles, ndJsonFile);
        }
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


    private void parseEobBundles(List<EobBundleDTO> eobBundles, Path ndjson) {
        final Iterator<EobBundleDTO> iterator = eobBundles.iterator();
        while (iterator.hasNext()) {
            final EobBundleDTO bundleDTO = iterator.next();
            try {
                final String payload = bundleDTO.toString() + System.lineSeparator();
                Files.write(ndjson, payload.getBytes(), StandardOpenOption.APPEND);
                iterator.remove();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
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
