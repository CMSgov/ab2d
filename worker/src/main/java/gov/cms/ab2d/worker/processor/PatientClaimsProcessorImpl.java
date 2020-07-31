package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.util.FHIRUtil;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.BeneficiarySearchEvent;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;
import static gov.cms.ab2d.filter.EOBLoadUtilities.isPartD;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClaimsProcessorImpl implements PatientClaimsProcessor {

    private final BFDClient bfdClient;
    private final FhirContext fhirContext;
    private final LogManager logManager;

    @Value("${claims.skipBillablePeriodCheck}")
    private boolean skipBillablePeriodCheck;
    @Value("${bfd.earliest.data.date:01/01/2020}")
    private String startDate;
    @Value("${bfd.earliest.data.date.special.contracts}")
    private String startDateSpecialContracts;
    @Value("${bfd.special.contracts}")
    private List<String> specialContracts;

    private static final OffsetDateTime START_CHECK = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);

    /**
     * Process the retrieval of patient explanation of benefit objects and write them
     * to a file using the writer
     */
    @Trace(async = true)
    @Async("patientProcessorThreadPool")
    public Future<Void> process(PatientClaimsRequest request, Map<String, ContractBeneficiaries.PatientDTO> map) {
        final Token token = request.getToken();
        token.link();

        int resourceCount = 0;

        String payload = "";
        try {
            // Retrieve the resource bundle of EOB objects
            var resources = getEobBundleResources(request, map);

            var jsonParser = fhirContext.newJsonParser();

            for (var resource : resources) {
                ++resourceCount;
                try {
                    payload = jsonParser.encodeResourceToString(resource) + System.lineSeparator();
                    request.getHelper().addData(payload.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    log.warn("Encountered exception while processing job resources: {}", e.getMessage());
                    handleException(request.getHelper(), payload, e);
                }
            }
        } catch (Exception e) {
            try {
                handleException(request.getHelper(), payload, e);
            } catch (IOException e1) {
                //should not happen - original exception will be thrown
                log.error("error during exception handling to write error record");
            }

            token.expire();
            return AsyncResult.forExecutionException(e);
        }

        log.debug("finished writing [{}] resources", resourceCount);

        token.expire();
        return new AsyncResult<>(null);
    }

    private void handleException(StreamHelper helper, String data, Exception e) throws IOException {
        var errMsg = ExceptionUtils.getRootCauseMessage(e);
        var operationOutcome = FHIRUtil.getErrorOutcome(errMsg);

        var jsonParser = fhirContext.newJsonParser();
        var payload = jsonParser.encodeResourceToString(operationOutcome) + System.lineSeparator();

        var byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        helper.addError(data);
    }

    private List<Resource> getEobBundleResources(PatientClaimsRequest request, Map<String, ContractBeneficiaries.PatientDTO> map) {
        ContractBeneficiaries.PatientDTO patient = request.getPatientDTO();
        OffsetDateTime attTime = request.getAttTime();

        OffsetDateTime start = OffsetDateTime.now();
        Bundle eobBundle;
        try {
            OffsetDateTime sinceTime = null;
            if (request.getSinceTime() == null) {
                if (request.getAttTime().isAfter(START_CHECK)) {
                    sinceTime = request.getAttTime();
                }
            } else {
                sinceTime = request.getSinceTime();
            }
            eobBundle = bfdClient.requestEOBFromServer(patient.getPatientId(), sinceTime);
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
        final List<Resource> resources = extractResources(request.getContractNum(), entries, patient.getDateRangesUnderContract(), attTime, map);

        while (eobBundle.getLink(Bundle.LINK_NEXT) != null) {
            eobBundle = bfdClient.requestNextBundleFromServer(eobBundle);
            final List<BundleEntryComponent> nextEntries = eobBundle.getEntry();
            resources.addAll(extractResources(request.getContractNum(), nextEntries, patient.getDateRangesUnderContract(), attTime, map));
        }

        log.debug("Bundle - Total: {} - Entries: {} ", eobBundle.getTotal(), entries.size());
        return resources;
    }

    private Date getStartDate(String contract) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

        String dateToUse = startDate;
        if (isContractSpecial(contract)) {
            dateToUse = startDateSpecialContracts;
        }
        Date date;
        try {
            date = sdf.parse(dateToUse);
        } catch (ParseException e) {
            LocalDateTime d = LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0);
            date = new Date(d.toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        return date;
    }

    private boolean isContractSpecial(String contract) {
        return this.specialContracts != null && !this.specialContracts.isEmpty() && specialContracts.contains(contract);
    }

    List<Resource> extractResources(String contractNum, List<BundleEntryComponent> entries,
                                    final List<FilterOutByDate.DateRange> dateRanges,
                                    OffsetDateTime attTime, Map<String, ContractBeneficiaries.PatientDTO> patientsMap) {
        if (attTime == null) {
            return new ArrayList<>();
        }
        long epochMilli = attTime.toInstant().toEpochMilli();
        Date attDate = new Date(epochMilli);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        final Date earliestDate = getStartDate(contractNum);
        return entries.stream()
                // Get the resource
                .map(BundleEntryComponent::getResource)
                // Get only the explanation of benefits
                .filter(resource -> resource.getResourceType() == ResourceType.ExplanationOfBenefit)
                // Filter by date
                .filter(resource -> skipBillablePeriodCheck || FilterOutByDate.valid((ExplanationOfBenefit) resource, attDate, earliestDate, dateRanges))
                // filter it
                .map(resource -> ExplanationOfBenefitTrimmer.getBenefit((ExplanationOfBenefit) resource))
                // Remove any empty values
                .filter(Objects::nonNull)
                // Remove Plan D
                .filter(resource -> !isPartD(resource))
                // Make sure the returned patient ID is actually part of the contract
                .filter(resource -> validPatientInContract(resource, patientsMap))
                // compile the list
                .collect(Collectors.toList());
    }

    /**
     * returns true if the patient is a valid member of a contract, false otherwise. If either value is empty,
     * it returns false
     *
     * @param benefit - The benefit to check
     * @param patients - the patient map containing the patient id & patient object
     * @return true if this patient is a member of the correct contract
     */
    boolean validPatientInContract(ExplanationOfBenefit benefit, Map<String, ContractBeneficiaries.PatientDTO> patients) {
        if (benefit == null || patients == null) {
            log.debug("Passed an invalid benefit or an invalid list of patients");
            return false;
        }
        String patientId = benefit.getPatient().getReference();
        if (patientId == null) {
            return false;
        }
        patientId = patientId.replaceFirst("Patient/", "");
        if (patients.get(patientId) == null) {
            log.error(patientId + " returned in EOB, but not a member of a contract");
            return false;
        }
        return true;
    }
}
