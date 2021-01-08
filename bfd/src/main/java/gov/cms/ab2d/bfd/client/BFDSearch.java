package gov.cms.ab2d.bfd.client;

import java.io.IOException;
import java.time.OffsetDateTime;

public interface BFDSearch {

    org.hl7.fhir.dstu3.model.Bundle searchEOB(String patientId, OffsetDateTime since, int pageSize, String bulkJobId) throws IOException;
}
