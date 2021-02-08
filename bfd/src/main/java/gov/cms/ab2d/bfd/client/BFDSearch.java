package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.fhir.Versions;
import org.hl7.fhir.instance.model.api.IBaseBundle;

import java.io.IOException;
import java.time.OffsetDateTime;

public interface BFDSearch {
    IBaseBundle searchEOB(String urlVariable, String patientId, OffsetDateTime since, int pageSize, String bulkJobId, Versions.FhirVersions version) throws IOException;
}
