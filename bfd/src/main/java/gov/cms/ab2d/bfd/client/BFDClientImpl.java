package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
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
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.validation.constraints.NotEmpty;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

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

    private Map<Versions.FhirVersions, IGenericClient> servers = new HashMap<>();

    @Value("${bfd.hicn.hash}")
    private String hicnHash;

    @Value("${bfd.mbi.hash}")
    private String mbiHash;

    @NotEmpty
    @Value("${bfd.hash.pepper}")
    private String bfdHashPepper;

    @Value("${bfd.hash.iter}")
    private int bfdHashIter;

    private final BFDSearch bfdSearch;
    private final HttpClient httpClient;
    private final Environment env;
    private final UrlValueResolver urlValueResolver;

    public BFDClientImpl(BFDSearch bfdSearch, HttpClient httpClient, Environment env, UrlValueResolver urlValueResolver) {
        this.bfdSearch = bfdSearch;
        this.httpClient = httpClient;
        this.env = env;
        this.urlValueResolver = urlValueResolver;
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

        IBaseBundle result = bfdSearch.searchEOB(Versions.getEnvVariable(version), patientID, sinceTime, pageSize, getJobId(), version);

        bfdSegment.end();

        return result;
    }

    /**
     * Pull all the data (specifically IDs) from the patient
     *
     * @param hicn - The HICN id
     * @return The bundle containing the patient object
     */
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public IBaseBundle requestPatientByHICN(Versions.FhirVersions version, String hicn) {
        String hicnHashVal = hashIdentifier(hicn, bfdHashPepper, bfdHashIter);
        var hicnHashEquals = generateHash(hicnHash, hicnHashVal);
        return clientSearch(version, hicnHashEquals);
    }

    private IBaseBundle clientSearch(Versions.FhirVersions version, @SuppressWarnings("rawtypes") ICriterion hashEquals) {
        return getClient(version).search()
                .forResource(SearchUtils.getPatientClass(version))
                .where(hashEquals)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_CLIENTID, BFDClient.BFD_CLIENT_ID)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_JOBID, getJobId())
                .withAdditionalHeader("IncludeIdentifiers", "true")
                .returnBundle(SearchUtils.getBundleClass(version))
                .encodedJson()
                .execute();
    }

    private IGenericClient getClient(Versions.FhirVersions version) {
        IGenericClient client = servers.get(version);
        if (client != null) {
            return client;
        }
        String url = Versions.getEnvVariable(version);
        String urlLocation = urlValueResolver.readMyProperty(env.getProperty(url));
        client = new FhirBfdServer(version).bfdFhirRestClient(httpClient, urlLocation);
        servers.put(version, client);
        return client;
    }

    @SuppressWarnings("rawtypes")
    private ICriterion generateHash(String hash, String hashVal) {
        return new TokenClientParam("identifier").exactly()
                .systemAndCode(hash, hashVal);
    }

    /**
     * Pull all the data (specifically IDs) from the patient
     *
     * @param mbi - The MBI id
     * @return The bundle containing the patient object
     */
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public IBaseBundle requestPatientByMBI(Versions.FhirVersions version, String mbi) {
        String hashVal = hashIdentifier(mbi, bfdHashPepper, bfdHashIter);
        var mbiHashEquals = generateHash(mbiHash, hashVal);
        return clientSearch(version, mbiHashEquals);
    }

    /**
     * Hash the identifier
     *
     * @param identifier - The patient identifier
     * @param pepper - The pepper to put into the algorithm
     * @param iterations - The number of iterations
     * @return the hashed value
     */
    private static String hashIdentifier(String identifier, String pepper, int iterations) {
        try {
            KeySpec keySpec = new PBEKeySpec(identifier.toCharArray(), Hex.decodeHex(pepper), iterations, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey secretKey = skf.generateSecret(keySpec);
            return Hex.encodeHexString(secretKey.getEncoded());
        } catch (DecoderException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ResourceNotFoundException("Could not hash identifier");
        }
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public IBaseBundle requestNextBundleFromServer(Versions.FhirVersions version, IBaseBundle bundle) {
        return getClient(version)
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

        return getClient(version).search()
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
            return getClient(version).capabilities()
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
