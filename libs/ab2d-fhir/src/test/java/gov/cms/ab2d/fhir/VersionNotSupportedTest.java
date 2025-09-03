package gov.cms.ab2d.fhir;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VersionNotSupportedTest {

  @Test
  void testConstructor() {
    assertThrows(RuntimeException.class, () -> {
      throw new VersionNotSupported("test");
    });
  }

}
