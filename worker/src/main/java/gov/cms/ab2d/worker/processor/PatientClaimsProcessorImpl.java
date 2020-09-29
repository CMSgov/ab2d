package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.BeneficiarySearchEvent;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.domainmodel.EobSearchResult;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
    private final LogManager logManager;

    @Value("${claims.skipBillablePeriodCheck}")
    private boolean skipBillablePeriodCheck;
    @Value("${bfd.earliest.data.date:01/01/2020}")
    private String startDate;
    @Value("${bfd.earliest.data.date.special.contracts}")
    private String startDateSpecialContracts;
    @Value("#{'${bfd.special.contracts}'.split(',')}")
    private List<String> specialContracts;

    private static final OffsetDateTime START_CHECK = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);

    /**
     * Process the retrieval of patient explanation of benefit objects and write them
     * to a file using the writer
     */
    @Trace(async = true)
    @Async("patientProcessorThreadPool")
    public Future<EobSearchResult> process(PatientClaimsRequest request) {
        final Token token = request.getToken();
        token.link();
        EobSearchResult result = new EobSearchResult();
        result.setJobId(request.getJob());
        result.setContractNum(request.getContractNum());
        try {
            result.setEobs(getEobBundleResources(request));
        } catch (Exception ex) {
            return AsyncResult.forExecutionException(ex);
        }
        token.expire();
        return new AsyncResult<>(result);
    }

    private List<ExplanationOfBenefit> getEobBundleResources(PatientClaimsRequest request) {
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
        final List<ExplanationOfBenefit> resources = extractResources(request.getContractNum(), entries, patient.getDateRangesUnderContract(), attTime);

        while (eobBundle.getLink(Bundle.LINK_NEXT) != null) {
            eobBundle = bfdClient.requestNextBundleFromServer(eobBundle);
            final List<BundleEntryComponent> nextEntries = eobBundle.getEntry();
            resources.addAll(extractResources(request.getContractNum(), nextEntries, patient.getDateRangesUnderContract(), attTime));
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

    List<ExplanationOfBenefit> extractResources(String contractNum, List<BundleEntryComponent> entries,
                                                final List<FilterOutByDate.DateRange> dateRanges,
                                                OffsetDateTime attTime) {
        if (attTime == null) {
            return new ArrayList<>();
        }
        long epochMilli = attTime.toInstant().toEpochMilli();
        Date attDate = new Date(epochMilli);
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
                // compile the list
                .collect(Collectors.toList());
    }
}
