package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.BeneficiaryAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.BfdClientAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.BfdClientAdapterImpl;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final String fileshare = "/Users/satheesh.pathiyil/Code/CMS/ab2d/test-data-share/";
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
            final String errMsg = String.format("Job %s is not in %s status. Skipping job", jobId, SUBMITTED);
            throw new IllegalArgumentException(errMsg);
        }

        job.setStatus(IN_PROGRESS);

        log.info("Job [{}] is now IN_PROGRESS", job.getId());
        jobRepository.save(job);
    }

    @Override
    public Job processJob(final String jobId) {

        log.info("Entering doLongRunningWork() ... {} ", jobId);

        final Job job = jobRepository.findByJobUuid(jobId);
        log.info("Found job : {} - {} - {} ", job.getId(), job.getJobUuid(), job.getStatus());

        final Sponsor sponsor = job.getUser().getSponsor();
        log.info("Found Sponsor : {} - {} - {} ", sponsor.getId(), sponsor.getLegalName(), sponsor.getOrgName());

        final List<Contract> attestedContracts = sponsor.getAttestedContracts();
        log.info("number of attested contracts : {}", attestedContracts.size());

        for (Contract contract : attestedContracts) {
            log.info("contract : {} ", contract.getContractName());
            processContract(job, contract);
        }

        return job;
    }

//    private List<Contract> getAttestedContracts(Sponsor sponsor) {
//        return sponsor.getContracts().stream()
//                .filter(c -> c.getAttestedOn() != null)
//                .collect(Collectors.toList());
//    }


    private void processContract(Job job, Contract contract) {
//        final Path outputDir = createOutputDir(job, contract);
        Path ndjson = createContractFile(job.getJobUuid(), contract);

        var patientsByContract = beneficiaryAdapter.getPatientsByContract(contract.getContractNumber());
        var patients = patientsByContract.getPatients();

        List<Future<BfdClientAdapterImpl.EobBundleDTO>> eobBundles =  new ArrayList<Future<BfdClientAdapterImpl.EobBundleDTO>>();

        int counter = 0;
        for (GetPatientsByContractResponse.PatientDTO patient : patients) {
            eobBundles.add(bfdClientAdapter.getEobBundle(patient.getPatientId()));

            ++counter;
            if (counter % 10 == 0) {
                parseEobBundles(eobBundles, ndjson);
            }
        }
    }



//    private Path createOutputDir(Job job, Contract contract) {
//        Path outputFilePath = Paths.get(fileshare, job.getJobUuid(), contract.getContractNumber());
//        try {
//            final Path outputDirectory = Files.createDirectories(outputFilePath);
//            log.info("new output directory created : {} ", outputDirectory.toAbsolutePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return outputFilePath;
//    }
//    private Path createOutputDir(Job job) {
//        Path outputFilePath = Paths.get(fileshare, job.getJobUuid());
//        try {
//            final Path outputDirectory = Files.createDirectories(outputFilePath);
//            log.info("new output directory created : {} ", outputDirectory.toAbsolutePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return outputFilePath;
//    }

    private Path createContractFile(final String jobUuid, Contract contract) {
        Path outputFilePath = Paths.get(fileshare, jobUuid);
        try {
            final Path outputDirectory = Files.createDirectories(outputFilePath);
            log.info("OutputDir: {} ", outputDirectory.toAbsolutePath());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        final Path outputDir = outputFilePath;
        final Path ndjsonPath = Path.of(outputDir.toString(), contract.getContractNumber() + ".ndjson");
        Path ndjson = null;
        try {
            ndjson = Files.createFile(ndjsonPath);
            log.info("file: {} ", ndjson.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ndjson;
    }

    private void parseEobBundles(List<Future<BfdClientAdapterImpl.EobBundleDTO>> eobBundles, Path ndjson) {
        final Iterator<Future<BfdClientAdapterImpl.EobBundleDTO>> iterator = eobBundles.iterator();
        while (iterator.hasNext()) {
            final Future<BfdClientAdapterImpl.EobBundleDTO> futureBundle = iterator.next();
//            if (futureBundle.isDone()) {
                try {
                    var bundleDTO = futureBundle.get();
                    log.info(" EOB Bundles : {}. Will write to file next.", bundleDTO.getPatientId());
                    // write to file next time
                    Files.write(ndjson, bundleDTO.toString().getBytes());

                    //                    log.info(" outputFile : {}", ndjson.toAbsolutePath());

//                    Path file = Paths.get(outputDir, bundleDTO.getPatientId());
//
//                    final Path file1 = Files.createFile(outputDir, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                    //write to ndjson file - single threaded
                    iterator.remove();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
//            }
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
