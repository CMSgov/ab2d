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
import org.hl7.fhir.dstu3.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.filter.EOBLoadUtilities.isPartD;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClaimsProcessorImpl implements PatientClaimsProcessor {
    @Value("${bfd.earliest.data.date:01/01/2020}")
    private String startDate;
    @Value("${bfd.earliest.data.date.special.contracts}")
    private String startDateSpecialContracts;
    @Value("#{'${bfd.special.contracts}'.split(',')}")
    private List<String> specialContracts;

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
        OffsetDateTime sinceDate = getSinceTime(request.getContractNum(), request.getAttTime());
        Bundle eobBundle;
        try {
            eobBundle = bfdClient.requestEOBFromServer(patient.getPatientId(), sinceDate);
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

        final List<Bundle.BundleEntryComponent> entries = eobBundle.getEntry();
        final List<Resource> resources = extractResources(entries);

        while (eobBundle.getLink(Bundle.LINK_NEXT) != null) {
            eobBundle = bfdClient.requestNextBundleFromServer(eobBundle);
            final List<Bundle.BundleEntryComponent> nextEntries = eobBundle.getEntry();
            resources.addAll(extractResources(nextEntries));
        }

        log.debug("Bundle - Total: {} - Entries: {} ", eobBundle.getTotal(), entries.size());
        return resources;
    }

    private OffsetDateTime getSinceTime(String contract, OffsetDateTime attestDate) {
        OffsetDateTime startDate = getStartDate(contract);
        if (attestDate.isAfter(startDate)) {
           return attestDate;
        }
        return startDate;
    }

    private OffsetDateTime getStartDate(String contract) {
        String dateToUse = startDate;
        if (isContractSpecial(contract)) {
            dateToUse = startDateSpecialContracts;
        }
        OffsetDateTime date = OffsetDateTime.parse(dateToUse + " 00:00:00:000+00:00",
                DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss:SSSXXXXX"));

        return date;
    }

    private boolean isContractSpecial(String contract) {
        return this.specialContracts != null && !this.specialContracts.isEmpty() && specialContracts.contains(contract);
    }

    List<Resource> extractResources(List<Bundle.BundleEntryComponent> entries) {
        return entries.stream()
                // Get the resource
                .map(Bundle.BundleEntryComponent::getResource)
                // Get only the explanation of benefits
                .filter(resource -> resource.getResourceType() == ResourceType.ExplanationOfBenefit)
                // map it to the actual type of Resource
                .map(resource -> ExplanationOfBenefitTrimmer.getBenefit((ExplanationOfBenefit) resource))
                // Remove any empty values
                .filter(Objects::nonNull)
                // Remove Plan D
                .filter(resource -> !isPartD(resource))
                // compile the list
                .collect(Collectors.toList());
    }
}
