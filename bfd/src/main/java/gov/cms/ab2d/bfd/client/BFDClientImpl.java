package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

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
    public Bundle requestEOBFromServer(String patientID) {
        log.debug(format("Attempting to fetch EOBs for patient ID %s from baseURL: %s", patientID,
                client.getServerBase()));
        return
                fetchBundle(ExplanationOfBenefit.class,
                        ExplanationOfBenefit.PATIENT.hasId(patientID), patientID);
    }


    @Override
    public Bundle requestNextBundleFromServer(Bundle bundle) throws ResourceNotFoundException {
        var nextURL = bundle.getLink(Bundle.LINK_NEXT).getUrl();
        log.debug(format("Attempting to fetch next bundle from url: %s", nextURL));
        return client
                .loadPage()
                .next(bundle)
                .execute();

    }


    /**
     * Read a FHIR Bundle from BlueButton. Limits the returned size by resourcesPerRequest.
     *
     * @param resourceClass - FHIR Resource class
     * @param criterion     - For the resource class the correct criterion that matches the
     *                      patientID
     * @param patientID     - id of patient
     * @return FHIR Bundle resource
     */
    private <T extends IBaseResource> Bundle fetchBundle(Class<T> resourceClass,
                                                         ICriterion<ReferenceClientParam> criterion,
                                                         String patientID) {
        final Bundle bundle = client.search()
                .forResource(resourceClass)
                .where(criterion)
                .count(pageSize)
                .returnBundle(Bundle.class)
                .execute();

        // Case where patientID does not exist at all
        if (!bundle.hasEntry()) {
            throw new ResourceNotFoundException("No patient found with ID: " + patientID);
        }
        return bundle;
    }

}
