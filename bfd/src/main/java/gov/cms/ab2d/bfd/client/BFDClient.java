package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.fhir.Versions;
import org.hl7.fhir.instance.model.api.IBaseConformance;

import java.time.OffsetDateTime;
import org.hl7.fhir.instance.model.api.IBaseBundle;

public interface BFDClient {
    String BFD_HDR_BULK_CLIENTID = "BULK-CLIENTID";
    String BFD_CLIENT_ID = "AB2D";
    String BFD_HDR_BULK_JOBID = "BULK-JOBID";

    IBaseBundle requestEOBFromServer(Versions.FhirVersions version, String patientID);
    IBaseBundle requestEOBFromServer(Versions.FhirVersions version, String patientID, OffsetDateTime sinceTime);
    IBaseBundle requestNextBundleFromServer(Versions.FhirVersions version, IBaseBundle bundle);

    /**
     * Request BFD for a list of all active patients in a contract for a specific month
     *
     * @param contractNumber - the PDP contract number
     * @param month - the month to search
     * @return Bundle of Patient Resources
     */
    IBaseBundle requestPartDEnrolleesFromServer(Versions.FhirVersions version, String contractNumber, int month);

    IBaseConformance capabilityStatement(Versions.FhirVersions version);

    ThreadLocal<String> BFD_BULK_JOB_ID = new ThreadLocal<>();
}

