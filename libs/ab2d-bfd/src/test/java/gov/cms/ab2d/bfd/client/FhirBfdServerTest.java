package gov.cms.ab2d.bfd.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;

import gov.cms.ab2d.fhir.FhirVersion;

class FhirBfdServerTest {

  @Test
  void testGetVersion() {
    assertEquals(FhirVersion.R4, new FhirBfdServer(FhirVersion.R4).getVersion());
  }

  @Test
  void testGetGenericClient() {
    FhirBfdServer fhirBfdServer = new FhirBfdServer(FhirVersion.R4);

    // We intentionally test this method twice to increase code coverage,
    // since the first call of the method mutates the object.
    assertNotNull(fhirBfdServer.getGenericClient(null, "test"));
    assertNotNull(fhirBfdServer.getGenericClient(null, "test"));
  }

}
