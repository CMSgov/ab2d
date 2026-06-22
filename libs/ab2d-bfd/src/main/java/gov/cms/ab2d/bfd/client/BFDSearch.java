package gov.cms.ab2d.bfd.client;

import gov.cms.ab2d.fhir.FhirVersion;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import java.io.IOException;

public interface BFDSearch {
    IBaseBundle searchEOB(BFDSearchDTO searchDTO) throws IOException;

    /** Same as {@link #searchEOB} except return raw byte array instead of parsing to IBaseBundle */
    byte[] searchEOBRaw(BFDSearchDTO searchDTO) throws IOException;

    /** Parse response from {@link #searchEOBRaw} */
    IBaseBundle parseBundle(FhirVersion version, byte[] response);
}
