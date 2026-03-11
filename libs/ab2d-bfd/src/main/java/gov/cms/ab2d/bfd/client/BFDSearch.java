package gov.cms.ab2d.bfd.client;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import java.io.IOException;

public interface BFDSearch {
    IBaseBundle searchEOB(BFDSearchDTO searchDTO) throws IOException;
}
