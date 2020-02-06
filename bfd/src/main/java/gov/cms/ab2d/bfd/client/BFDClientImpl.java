package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
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

/**
 * Credits: most of the code in this class has been copied over from https://github
 * .com/CMSgov/dpc-app
 */
@Component
@PropertySource("classpath:application.bfd.properties")
@Slf4j
public class BFDClientImpl implements BFDClient {

    @Value("${bfd.pagesize}")
    private int pageSize;

    private IGenericClient client;

    @Value("${bfd.hicn.hash}")
    private String hcinHash;

    @NotEmpty
    @Value("${bfd.hash.pepper}")
    private String bfdHashPepper;

    @Value("${bfd.hash.iter}")
    private int bfdHashIter;

    public BFDClientImpl(IGenericClient bfdFhirRestClient) {
        this.client = bfdFhirRestClient;
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
     * @return {@link Bundle} Containing a number (possibly 0) of {@link ExplanationOfBenefit}
     * objects
     * @throws ResourceNotFoundException when the requested patient does not exist
     */
    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public Bundle requestEOBFromServer(String patientID) {
        return client.search()
                .forResource(ExplanationOfBenefit.class)
                .where(ExplanationOfBenefit.PATIENT.hasId(patientID))
                .count(pageSize)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
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
    public Bundle requestPatientFromServer(String hicn) {
        String hicnHashVal = hashHICN(hicn, bfdHashPepper, bfdHashIter);
        ICriterion hicnHashEquals = new TokenClientParam("identifier").exactly()
                .systemAndCode(hcinHash, hicnHashVal);
        return client.search()
                .forResource(Patient.class)
                .where(hicnHashEquals)
                .withAdditionalHeader("IncludeIdentifiers", "true")
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
    }

    /**
     * Hash the HICN Id
     *
     * @param hicn - The HCIN id
     * @param pepper - The pepper to put into the algorithm
     * @param iterations - The number of iterations
     * @return the hashed value
     */
    private static String hashHICN(String hicn, String pepper, int iterations) {
        KeySpec keySpec = new PBEKeySpec(hicn.toCharArray(), Hex.decode(pepper), iterations, 256);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey secretKey = skf.generateSecret(keySpec);
            return Hex.toHexString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ResourceNotFoundException("Could not hash HICN");
        }
    }

    @Override
    @Retryable(
            maxAttemptsExpression = "${bfd.retry.maxAttempts:3}",
            backoff = @Backoff(delayExpression = "${bfd.retry.backoffDelay:250}", multiplier = 2),
            exclude = { ResourceNotFoundException.class }
    )
    public Bundle requestNextBundleFromServer(Bundle bundle) {
        return client
                .loadPage()
                .next(bundle)
                .encodedJson()
                .execute();
    }
}
