package gov.cms.ab2d.bfd.client;

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

    @Value("${bfd.eob.pagesize}")
    private int pageSize;

    @Value("${bfd.contract.to.bene.pagesize}")
    private int contractToBenePageSize;

    private final BFDSearch bfdSearch;
    private final BfdClientVersions bfdClientVersions;

    public BFDClientImpl(BFDSearch bfdSearch, BfdClientVersions bfdClientVersions) {
        this.bfdSearch = bfdSearch;
        this.bfdClientVersions = bfdClientVersions;
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
    public IBaseBundle requestEOBFromServer(Versions.FhirVersions version, String patientID) {
        return requestEOBFromServer(version, patientID, null);
    }

    /**
     * Queries Blue Button server for Explanations of Benefit associated with a given patient
     * similar to {@link #requestEOBFromServer(Versions.FhirVersions, String)} but includes a date filter in which the
     * _lastUpdated date must be after
     * <p>
     *
     * @param version The FHIR version
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
    public IBaseBundle requestEOBFromServer(Versions.FhirVersions version, String patientID, OffsetDateTime sinceTime) {
        final Segment bfdSegment = NewRelic.getAgent().getTransaction().startSegment("BFD Call for patient with patient ID " + patientID +
                " using since " + sinceTime);
        bfdSegment.setMetricName("RequestEOB");

        IBaseBundle result = bfdSearch.searchEOB(patientID, sinceTime, pageSize, getJobId(), version);

        bfdSegment.end();

        return result;
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public IBaseBundle requestNextBundleFromServer(Versions.FhirVersions version, IBaseBundle bundle) {
        return bfdClientVersions.getClient(version)
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
    public IBaseBundle requestPartDEnrolleesFromServer(Versions.FhirVersions version, String contractNumber, int month) {
        var monthParameter = createMonthParameter(month);
        var theCriterion = new TokenClientParam("_has:Coverage.extension")
                .exactly()
                .systemAndIdentifier(monthParameter, contractNumber);

        return bfdClientVersions.getClient(version).search()
                .forResource(SearchUtils.getPatientClass(version))
                .where(theCriterion)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_CLIENTID, BFDClient.BFD_CLIENT_ID)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_JOBID, getJobId())
                .withAdditionalHeader("IncludeIdentifiers", "mbi")
                .count(contractToBenePageSize)
                .returnBundle(SearchUtils.getBundleClass(version))
                .encodedJson()
                .execute();
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public IBaseConformance capabilityStatement(Versions.FhirVersions version) {
        try {
            Class<? extends IBaseConformance> resource = MetaDataUtils.getCapabilityClass(version);
            return bfdClientVersions.getClient(version).capabilities()
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
}
