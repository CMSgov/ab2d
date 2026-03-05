package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.fhir.FhirVersion;
import org.hl7.fhir.instance.model.api.IBaseBundle;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

public interface BFDSearch {
    IBaseBundle searchEOB(long patientId, OffsetDateTime since, OffsetDateTime until, List<String> serviceDates, int pageSize, String bulkJobId, FhirVersion version, String contractNum) throws IOException;
}
