package gov.cms.ab2d.common.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InvalidContractExceptionTest {

  @Test
  void test() {
    assertThrows(RuntimeException.class, () -> {
      throw new InvalidContractException("test");
    });
  }

}
