package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.fhir.Versions.FhirVersions;
import org.hl7.fhir.instance.model.api.IBaseBundle;

import java.io.IOException;
import java.time.OffsetDateTime;

public interface BFDSearch {
    IBaseBundle searchEOB(String patientId, OffsetDateTime since, int pageSize, String bulkJobId, FhirVersions version) throws IOException;
}
