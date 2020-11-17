package gov.cms.ab2d.bfd.client;


import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import java.time.OffsetDateTime;


public interface BFDClient {
    String BFD_HDR_BULK_CLIENTID = "BULK-CLIENTID";
    String BFD_CLIENT_ID = "AB2D";
    String BFD_HDR_BULK_JOBID = "BULK-JOBID";

    Bundle requestEOBFromServer(String patientID);
    Bundle requestEOBFromServer(String patientID, OffsetDateTime sinceTime);
    Bundle requestNextBundleFromServer(Bundle bundle);
    Bundle requestPatientByHICN(String patientId);
    Bundle requestPatientByMBI(String patientId);

    /**
     * Request BFD for a list of all active patients in a contract for a specific month
     *
     * @param contractNumber
     * @param month
     * @return Bundle of Patient Resources
     */
    Bundle requestPartDEnrolleesFromServer(String contractNumber, int month);

    CapabilityStatement capabilityStatement();

    ThreadLocal<String> BFD_BULK_JOB_ID = new ThreadLocal<>();
}

