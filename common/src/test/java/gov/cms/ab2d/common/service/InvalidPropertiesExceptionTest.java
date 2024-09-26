package gov.cms.ab2d.common.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InvalidPropertiesExceptionTest {

  @Test
  void test() {
    assertThrows(RuntimeException.class, () -> {
      throw new InvalidPropertiesException("test");
    });
  }

}
