package gov.cms.ab2d.bfd.client;


import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import java.time.OffsetDateTime;


public interface BFDClient {
    Bundle requestEOBFromServer(String patientID);
    Bundle requestEOBFromServer(String patientID, OffsetDateTime sinceTime);
    Bundle requestNextBundleFromServer(Bundle bundle);
    Bundle requestPatientFromServer(String patientId);

    /**
     * Request BFD for a list of all active patients in a contract for a specific month
     *
     * @param contractNumber
     * @param month
     * @return Bundle of Patient Resources
     */
    Bundle requestPartDEnrolleesFromServer(String contractNumber, int month);

    CapabilityStatement capabilityStatement();
}

