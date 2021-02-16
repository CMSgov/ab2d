package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import gov.cms.ab2d.fhir.MetaDataUtils;
import gov.cms.ab2d.fhir.SearchUtils;
import gov.cms.ab2d.fhir.Versions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Credits: most of the code in this class has been copied over from https://github
 * .com/CMSgov/dpc-app
 */
@Component
@PropertySource("classpath:application.bfd.properties")
@Slf4j
public class BFDClientImpl implements BFDClient {

    public static final String PTDCNTRCT_URL_PREFIX = "https://bluebutton.cms.gov/resources/variables/ptdcntrct";
    public static final String YEAR_URL_PREFIX = "https://bluebutton.cms.gov/resources/variables/rfrnc_yr";

    private final IGenericClient client;
    private final FhirContext fhirContext;
    private final BFDSearch bfdSearch;
    private final int pageSize;
    private final int contractToBenePageSize;

    public BFDClientImpl(IGenericClient client, FhirContext fhirContext, BFDSearch bfdSearch,
                         @Value("${bfd.eob.pagesize}") int pageSize,
                         @Value("${bfd.contract.to.bene.pagesize}") int contractToBenePageSize) {
        this.client = client;
        this.fhirContext = fhirContext;
        this.bfdSearch = bfdSearch;
        this.pageSize = pageSize;
        this.contractToBenePageSize = contractToBenePageSize;
    }

    /**
     * Queries Blue Button server for Explanations of Benefit associated with a given patient
     * <p>
     * There are two edge cases to consider when pulling EoB data given a patientID:
     * 1. No patient with the given ID exists: if this is the case, BlueButton should return a
     * Bundle with no
     * entry, i.e. ret.hasEntry() will evaluate to false. For this case, the method will throw a
     * {@link ResourceNotFoundException}
     * <p>
     * 2. A patient with the given ID exists, but has no associated EoB records: if this is the
     * case, BlueButton should
     * return a Bundle with an entry of size 0, i.e. ret.getEntry().size() == 0. For this case,
     * the method simply
     * returns the Bundle it received from BlueButton to the caller, and the caller is
     * responsible for handling Bundles
     * that contain no EoBs.
     *
     * @param patientID The requested patient's ID
     * @return {@link org.hl7.fhir.instance.model.api.IBaseBundle} Containing a number (possibly 0) of Resources
     * objects
     * @throws ResourceNotFoundException when the requested patient does not exist
     */
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public IBaseBundle requestEOBFromServer(String patientID) {
        return requestEOBFromServer(patientID, null);
    }

    /**
     * Queries Blue Button server for Explanations of Benefit associated with a given patient
     * similar to {@link #requestEOBFromServer(String)} but includes a date filter in which the
     * _lastUpdated date must be after
     * <p>
     *
     * @param patientID The requested patient's ID
     * @param sinceTime The start date for the request
     * @return {@link IBaseBundle} Containing a number (possibly 0) of Resources
     * objects
     * @throws ResourceNotFoundException when the requested patient does not exist
     */
    @SneakyThrows
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public IBaseBundle requestEOBFromServer(String patientID, OffsetDateTime sinceTime) {
        final Segment bfdSegment = NewRelic.getAgent().getTransaction().startSegment("BFD Call for patient with patient ID " + patientID +
                " using since " + sinceTime);
        bfdSegment.setMetricName("RequestEOB");

        IBaseBundle result = bfdSearch.searchEOB(patientID, sinceTime, pageSize, getJobId(), getVersion());

        bfdSegment.end();

        return result;
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public IBaseBundle requestNextBundleFromServer(IBaseBundle bundle) {
        return client
                .loadPage()
                .next(bundle)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_CLIENTID, BFDClient.BFD_CLIENT_ID)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_JOBID, getJobId())
                .withAdditionalHeader("IncludeIdentifiers", "mbi")
                .encodedJson()
                .execute();
    }

    private String getJobId() {
        var jobId = BFDClient.BFD_BULK_JOB_ID.get();
        if (jobId == null) {
            log.warn("BFD Bulk Job Id not set: " + new Throwable());  // Capture the stack trace for diagnosis
            jobId = "UNKNOWN";
        }
        return jobId;
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class, InvalidRequestException.class }
    )
    public IBaseBundle requestPartDEnrolleesFromServer(String contractNumber, int month) {
        var monthParameter = createMonthParameter(month);
        var monthCriterion = new TokenClientParam("_has:Coverage.extension")
                .exactly()
                .systemAndIdentifier(monthParameter, contractNumber);

        return client.search()
                .forResource(SearchUtils.getPatientClass(getVersion()))
                .where(monthCriterion)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_CLIENTID, BFDClient.BFD_CLIENT_ID)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_JOBID, getJobId())
                .withAdditionalHeader("IncludeIdentifiers", "mbi")
                .count(contractToBenePageSize)
                .returnBundle(SearchUtils.getBundleClass(getVersion()))
                .encodedJson()
                .execute();
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class, InvalidRequestException.class }
    )
    public IBaseBundle requestPartDEnrolleesFromServer(String contractNumber, int month, int year) {
        var monthParameter = createMonthParameter(month);
        var monthCriterion = new TokenClientParam("_has:Coverage.extension")
                .exactly()
                .systemAndIdentifier(monthParameter, contractNumber);
        var yearCriterion = new TokenClientParam("_has:Coverage.rfrncyr")
                .exactly()
                .systemAndIdentifier(YEAR_URL_PREFIX, createYearParameter(year));


        return client.search()
                .forResource(SearchUtils.getPatientClass(getVersion()))
                .where(monthCriterion)
                .and(yearCriterion)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_CLIENTID, BFDClient.BFD_CLIENT_ID)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_JOBID, getJobId())
                .withAdditionalHeader("IncludeIdentifiers", "mbi")
                .count(contractToBenePageSize)
                .returnBundle(SearchUtils.getBundleClass(getVersion()))
                .encodedJson()
                .prettyPrint()
                .execute();
    }
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public IBaseConformance capabilityStatement() {
        try {
            Class<? extends IBaseConformance> resource = MetaDataUtils.getCapabilityClass(getVersion());
            return client.capabilities()
                .ofType(resource)
                .execute();
        } catch (Exception ex) {
            return null;
        }
    }

    private String createMonthParameter(int month) {
        final String zeroPaddedMonth = StringUtils.leftPad("" + month, 2, '0');
        return PTDCNTRCT_URL_PREFIX + zeroPaddedMonth;
    }

    // Pad year to expected four digits
    private String createYearParameter(int year) {
        return StringUtils.leftPad("" + year, 4, '0');
    }

    @Override
    public Versions.FhirVersions getVersion() {
        try {
            return Versions.getVersion(fhirContext);
        } catch (Exception ex) {
            log.error("Invalid version", ex);
            return Versions.FhirVersions.STU3;
        }
    }
}
