package gov.cms.ab2d.bfd.client;

import java.time.OffsetDateTime;


public interface BFDClient {
    String BFD_HDR_BULK_CLIENTID = "BULK-CLIENTID";
    String BFD_CLIENT_ID = "AB2D";
    String BFD_HDR_BULK_JOBID = "BULK-JOBID";

    org.hl7.fhir.dstu3.model.Bundle requestEOBFromServer(String patientID);
    org.hl7.fhir.dstu3.model.Bundle requestEOBFromServer(String patientID, OffsetDateTime sinceTime);
    org.hl7.fhir.dstu3.model.Bundle requestNextBundleFromServer(org.hl7.fhir.dstu3.model.Bundle bundle);
    org.hl7.fhir.dstu3.model.Bundle requestPatientByHICN(String patientId);
    org.hl7.fhir.dstu3.model.Bundle requestPatientByMBI(String patientId);

    /**
     * Request BFD for a list of all active patients in a contract for a specific month
     *
     * @param contractNumber
     * @param month
     * @return Bundle of Patient Resources
     */
    org.hl7.fhir.dstu3.model.Bundle requestPartDEnrolleesFromServer(String contractNumber, int month);

    org.hl7.fhir.dstu3.model.CapabilityStatement capabilityStatement();

    ThreadLocal<String> BFD_BULK_JOB_ID = new ThreadLocal<>();
}

