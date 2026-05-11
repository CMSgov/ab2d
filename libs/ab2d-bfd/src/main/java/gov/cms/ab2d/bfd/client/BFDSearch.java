package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.fhir.FhirVersion;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import java.io.IOException;

public interface BFDSearch {
    IBaseBundle searchEOB(BFDSearchDTO searchDTO) throws IOException;

    default byte[] searchEOBWithoutParseBundle(BFDSearchDTO searchDTO) throws IOException {
        return null;
    }

    default IBaseBundle parseBundle(FhirVersion version, byte[] response) {
        return null;
    }
}
