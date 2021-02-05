package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.fhir.Versions;
import org.hl7.fhir.instance.model.api.IBaseConformance;

import java.time.OffsetDateTime;
import org.hl7.fhir.instance.model.api.IBaseBundle;

public interface BFDClient {
    String BFD_HDR_BULK_CLIENTID = "BULK-CLIENTID";
    String BFD_CLIENT_ID = "AB2D";
    String BFD_HDR_BULK_JOBID = "BULK-JOBID";

    IBaseBundle requestEOBFromServer(String patientID);
    IBaseBundle requestEOBFromServer(String patientID, OffsetDateTime sinceTime);
    IBaseBundle requestNextBundleFromServer(IBaseBundle bundle);
    IBaseBundle requestPatientByHICN(String patientId);
    IBaseBundle requestPatientByMBI(String patientId);

    /**
     * Request BFD for a list of all active patients in a contract for a specific month
     *
     * @param contractNumber
     * @param month
     * @return Bundle of Patient Resources
     */
    IBaseBundle requestPartDEnrolleesFromServer(String contractNumber, int month);

    IBaseConformance capabilityStatement();

    Versions.FhirVersions getVersion();

    ThreadLocal<String> BFD_BULK_JOB_ID = new ThreadLocal<>();
}

