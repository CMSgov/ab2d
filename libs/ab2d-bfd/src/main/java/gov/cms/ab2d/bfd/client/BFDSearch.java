package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.bfd.dto;
import gov.cms.ab2d.fhir.FhirVersion;
import org.hl7.fhir.instance.model.api.IBaseBundle;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

public interface BFDSearch {
    IBaseBundle searchEOB(BFDSearchDTO searchDTO) throws IOException;
}
