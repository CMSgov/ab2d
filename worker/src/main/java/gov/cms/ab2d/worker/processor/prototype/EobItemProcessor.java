package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.worker.config.SearchConfig;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.PatientClaimsRequest;
import gov.cms.ab2d.worker.service.ContractWorkerClient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@StepScope
public class EobItemProcessor implements ItemProcessor<CoverageSummary, List<IBaseResource>> {
    // set false to simulate a failure
    private volatile boolean alreadyFailedOnce = true;
    private int patientToFail = 950;

    private final PatientClaimsProcessor patientClaimsProcessor;
    private final PatientClaimsRequest request;
    private final long itemDelayMs;

    public EobItemProcessor(
            PatientClaimsProcessor patientClaimsProcessor,
            JobRepository jobRepository,
            ContractWorkerClient contractWorkerClient,
            SearchConfig searchConfig,
            @Value("#{jobParameters['jobUuid']}") String jobUuid,
            @Value("${pause-resume.prototype.item-delay-ms:0}") long itemDelayMs) {
        this.patientClaimsProcessor = patientClaimsProcessor;
        this.itemDelayMs = itemDelayMs;
        Job job = jobRepository.findByJobUuid(jobUuid);
        ContractDTO contract = contractWorkerClient.getContractByContractNumber(job.getContractNumber());

        this.request = new PatientClaimsRequest(
                List.of(),
                contract.getAttestedOn(),
                job.getSince(),
                job.getUntil(),
                job.getServiceDates(),
                job.getOrganization(),
                jobUuid,
                job.getContractNumber(),
                contract.getContractType(),
                job.getFhirVersion(),
                searchConfig.getEfsMount()
        );
    }

    @Override
    public List<IBaseResource> process(CoverageSummary patient) throws InterruptedException {
        // optional artificial slowdown so a running job stays alive long enough to interrupt in tests
        if (itemDelayMs > 0) {
            Thread.sleep(itemDelayMs);
        }
        if (patient.getIdentifiers().getPatientIdV3() == patientToFail && !alreadyFailedOnce) {
            alreadyFailedOnce = true;
            throw new RuntimeException("Simulated an expected failure on patient");
        }
        List<IBaseResource> eobs = patientClaimsProcessor.getEobBundleResources(request, patient);
        return (eobs == null || eobs.isEmpty()) ? null : eobs;
    }
}
