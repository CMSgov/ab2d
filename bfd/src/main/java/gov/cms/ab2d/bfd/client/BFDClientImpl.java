package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
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

    private final IGenericClient client;

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

    public BFDClientImpl(IGenericClient client, BFDSearch bfdSearch) {
        this.client = client;
        this.bfdSearch = bfdSearch;
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
     * @return {@link org.hl7.fhir.dstu3.model.Bundle} Containing a number (possibly 0) of {@link org.hl7.fhir.dstu3.model.ExplanationOfBenefit}
     * objects
     * @throws ResourceNotFoundException when the requested patient does not exist
     */
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public org.hl7.fhir.dstu3.model.Bundle requestEOBFromServer(String patientID) {
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
     * @return {@link org.hl7.fhir.dstu3.model.Bundle} Containing a number (possibly 0) of {@link org.hl7.fhir.dstu3.model.ExplanationOfBenefit}
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
    public org.hl7.fhir.dstu3.model.Bundle requestEOBFromServer(String patientID, OffsetDateTime sinceTime) {
        final Segment bfdSegment = NewRelic.getAgent().getTransaction().startSegment("BFD Call for patient with patient ID " + patientID +
                " using since " + sinceTime);
        bfdSegment.setMetricName("RequestEOB");

        org.hl7.fhir.dstu3.model.Bundle result = bfdSearch.searchEOB(patientID, sinceTime, pageSize, getJobId());

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
    public org.hl7.fhir.dstu3.model.Bundle requestPatientByHICN(String hicn) {
        String hicnHashVal = hashIdentifier(hicn, bfdHashPepper, bfdHashIter);
        var hicnHashEquals = generateHash(hicnHash, hicnHashVal);
        return clientSearch(hicnHashEquals);
    }

    private org.hl7.fhir.dstu3.model.Bundle clientSearch(@SuppressWarnings("rawtypes") ICriterion hashEquals) {
        return client.search()
                .forResource(org.hl7.fhir.dstu3.model.Patient.class)
                .where(hashEquals)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_CLIENTID, BFDClient.BFD_CLIENT_ID)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_JOBID, getJobId())
                .withAdditionalHeader("IncludeIdentifiers", "true")
                .returnBundle(org.hl7.fhir.dstu3.model.Bundle.class)
                .encodedJson()
                .execute();
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
    public org.hl7.fhir.dstu3.model.Bundle requestPatientByMBI(String mbi) {
        String hashVal = hashIdentifier(mbi, bfdHashPepper, bfdHashIter);
        var mbiHashEquals = generateHash(mbiHash, hashVal);
        return clientSearch(mbiHashEquals);
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
    public org.hl7.fhir.dstu3.model.Bundle requestNextBundleFromServer(org.hl7.fhir.dstu3.model.Bundle bundle) {
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
    public org.hl7.fhir.dstu3.model.Bundle requestPartDEnrolleesFromServer(String contractNumber, int month) {
        var monthParameter = createMonthParameter(month);
        var theCriterion = new TokenClientParam("_has:Coverage.extension")
                .exactly()
                .systemAndIdentifier(monthParameter, contractNumber);

        return client.search()
                .forResource(org.hl7.fhir.dstu3.model.Patient.class)
                .where(theCriterion)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_CLIENTID, BFDClient.BFD_CLIENT_ID)
                .withAdditionalHeader(BFDClient.BFD_HDR_BULK_JOBID, getJobId())
                .withAdditionalHeader("IncludeIdentifiers", "mbi")
                .count(contractToBenePageSize)
                .returnBundle(org.hl7.fhir.dstu3.model.Bundle.class)
                .encodedJson()
                .execute();
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public org.hl7.fhir.dstu3.model.CapabilityStatement capabilityStatement() {
        return client.capabilities()
                .ofType(org.hl7.fhir.dstu3.model.CapabilityStatement.class)
                .execute();
    }

    private String createMonthParameter(int month) {
        final String zeroPaddedMonth = StringUtils.leftPad("" + month, 2, '0');
        return PTDCNTRCT_URL_PREFIX + zeroPaddedMonth;
    }
}
