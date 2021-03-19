package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.fhir.FhirVersion;
import org.hl7.fhir.instance.model.api.IBaseConformance;

import java.time.OffsetDateTime;
import org.hl7.fhir.instance.model.api.IBaseBundle;

public interface BFDClient {
    String BFD_HDR_BULK_CLIENTID = "BULK-CLIENTID";
    String BFD_CLIENT_ID = "AB2D";
    String BFD_HDR_BULK_JOBID = "BULK-JOBID";

    IBaseBundle requestEOBFromServer(FhirVersion version, String patientID);
    IBaseBundle requestEOBFromServer(FhirVersion version, String patientID, OffsetDateTime sinceTime);
    IBaseBundle requestNextBundleFromServer(FhirVersion version, IBaseBundle bundle);

    /**
     * Request BFD for a list of all active patients in a contract for a specific month
     *
     * This month will be accurate for the current calendar year only
     *
     * @param contractNumber contract number (Z0000)
     * @param month month of the year
     * @return Bundle of Patient Resources
     */
    @Deprecated
    IBaseBundle requestPartDEnrolleesFromServer(FhirVersion version, String contractNumber, int month);

    IBaseBundle requestPartDEnrolleesFromServer(FhirVersion version, String contractNumber, int month, int year);

    IBaseConformance capabilityStatement(FhirVersion version);

    ThreadLocal<String> BFD_BULK_JOB_ID = new ThreadLocal<>();
}

