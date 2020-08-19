package gov.cms.ab2d.bfd.client;

import org.hl7.fhir.dstu3.model.Bundle;

import java.io.IOException;
import java.time.OffsetDateTime;

public interface BFDSearch {

    Bundle searchEOB(String patientId, OffsetDateTime since, int pageSize) throws IOException;
}
