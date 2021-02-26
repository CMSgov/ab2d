package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.BeneficiarySearchEvent;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.common.util.FilterOutByDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.*;
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

    private List<IBaseResource> getEobBundleResources(PatientClaimsRequest request) {
        CoverageSummary patient = request.getCoverageSummary();
        String beneficiaryId = patient.getIdentifiers().getBeneficiaryId();
        OffsetDateTime attTime = request.getAttTime();

        OffsetDateTime start = OffsetDateTime.now();
        IBaseBundle eobBundle;
        try {
            OffsetDateTime sinceTime = null;
            if (request.getSinceTime() == null) {
                if (request.getAttTime().isAfter(START_CHECK)) {
                    sinceTime = request.getAttTime();
                }
            } else {
                sinceTime = request.getSinceTime();
            }
            BFDClient.BFD_BULK_JOB_ID.set(request.getJob());
            eobBundle = bfdClient.requestEOBFromServer(request.getVersion(), patient.getIdentifiers().getBeneficiaryId(), sinceTime);
            logManager.log(LogManager.LogType.KINESIS,
                    new BeneficiarySearchEvent(request.getOrganization(), request.getJob(), request.getContractNum(),
                            start, OffsetDateTime.now(),
                            beneficiaryId,
                            "SUCCESS"));

        } catch (Exception ex) {
            logManager.log(LogManager.LogType.KINESIS,
                    new BeneficiarySearchEvent(request.getOrganization(), request.getJob(), request.getContractNum(),
                            start, OffsetDateTime.now(),
                            beneficiaryId,
                            "ERROR: " + ex.getMessage()));
            throw ex;
        } finally {
            BFDClient.BFD_BULK_JOB_ID.remove();
        }

        final List<IBaseBackboneElement> entries = BundleUtils.getEntries(eobBundle);
        final List<IBaseResource> resources = extractResources(request.getContractNum(), entries, patient.getDateRanges(), attTime);

        while (BundleUtils.getNextLink(eobBundle) != null) {
            eobBundle = bfdClient.requestNextBundleFromServer(request.getVersion(), eobBundle);
            final List<IBaseBackboneElement> nextEntries = BundleUtils.getEntries(eobBundle);
            resources.addAll(extractResources(request.getContractNum(), nextEntries, patient.getDateRanges(), attTime));
        }

        log.debug("Bundle - Total: {} - Entries: {} ", BundleUtils.getTotal(eobBundle), entries.size());
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

    List<IBaseResource> extractResources(String contractNum,
                                     List<IBaseBackboneElement> entries,
                                     final List<FilterOutByDate.DateRange> dateRanges,
                                     OffsetDateTime attTime) {
        if (attTime == null) {
            return new ArrayList<>();
        }
        long epochMilli = attTime.toInstant().toEpochMilli();
        Date attDate = new Date(epochMilli);
        final Date earliestDate = getStartDate(contractNum);
        return BundleUtils.getEobResources(entries).stream()
                // Filter by date
                .filter(resource -> skipBillablePeriodCheck || FilterOutByDate.valid(resource, attDate, earliestDate, dateRanges))
                // filter it
                .map(resource -> ExplanationOfBenefitTrimmer.getBenefit(resource))
                // Remove any empty values
                .filter(Objects::nonNull)
                // Remove Plan D
                .filter(resource -> !EobUtils.isPartD(resource))
                // compile the list
                .collect(Collectors.toList());
    }
}
