package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.BeneficiarySearchEvent;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.domainmodel.EobSearchResponse;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.filter.EOBLoadUtilities.isPartD;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClaimsProcessorImpl implements PatientClaimsProcessor {

    private final BFDClient bfdClient;
    private final LogManager logManager;

    /**
     * Process the retrieval of patient explanation of benefit objects and write them
     * to a file using the writer
     */
    @Trace(async = true)
    @Async("patientProcessorThreadPool")
    public Future<EobSearchResponse> process(PatientClaimsRequest request) {
        final Token token = request.getToken();
        token.link();
        List<Resource> resources = getEobBundleResources(request);
        token.expire();
        return new AsyncResult<>(new EobSearchResponse(request.getPatientDTO(), resources));
    }

    private List<Resource> getEobBundleResources(PatientClaimsRequest request) {
        ContractBeneficiaries.PatientDTO patient = request.getPatientDTO();
        OffsetDateTime start = OffsetDateTime.now();
        Bundle eobBundle;
        try {
            eobBundle = bfdClient.requestEOBFromServer(patient.getPatientId());
            logManager.log(LogManager.LogType.KINESIS,
                    new BeneficiarySearchEvent(request.getUser(), request.getJob(), request.getContractNum(),
                            start, OffsetDateTime.now(),
                            request.getPatientDTO() != null ? request.getPatientDTO().getPatientId() : null,
                            "SUCCESS"));

        } catch (Exception ex) {
            logManager.log(LogManager.LogType.KINESIS,
                    new BeneficiarySearchEvent(request.getUser(), request.getJob(), request.getContractNum(),
                            start, OffsetDateTime.now(),
                            request.getPatientDTO() != null ? request.getPatientDTO().getPatientId() : null,
                            "ERROR: " + ex.getMessage()));
            throw ex;
        }

        final List<BundleEntryComponent> entries = eobBundle.getEntry();
        final List<Resource> resources = extractResources(entries);

        while (eobBundle.getLink(Bundle.LINK_NEXT) != null) {
            eobBundle = bfdClient.requestNextBundleFromServer(eobBundle);
            final List<BundleEntryComponent> nextEntries = eobBundle.getEntry();
            resources.addAll(extractResources(nextEntries));
        }

        log.debug("Bundle - Total: {} - Entries: {} ", eobBundle.getTotal(), entries.size());
        return resources;
    }

    List<Resource> extractResources(List<BundleEntryComponent> entries) {
        return entries.stream()
                // Get the resource
                .map(BundleEntryComponent::getResource)
                // Get only the explanation of benefits
                .filter(resource -> resource.getResourceType() == ResourceType.ExplanationOfBenefit)
                // Filter by date
                // .filter(resource -> skipBillablePeriodCheck || FilterOutByDate.valid((ExplanationOfBenefit) resource, attDate, earliestDate, dateRanges))
                // filter it
                .map(resource -> ExplanationOfBenefitTrimmer.getBenefit((ExplanationOfBenefit) resource))
                // Remove any empty values
                .filter(Objects::nonNull)
                // Remove Plan D
                .filter(resource -> !isPartD(resource))
                // Make sure the returned patient ID is actually part of the contract
                //.filter(resource -> validPatientInContract(resource, patientsMap))
                // compile the list
                .collect(Collectors.toList());
    }
}
