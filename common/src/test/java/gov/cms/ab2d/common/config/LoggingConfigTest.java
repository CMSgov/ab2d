package gov.cms.ab2d.common.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class LoggingConfigTest {

  @Test
  void testConstructor() {
    // jsonLogging is null
    assertDoesNotThrow(
      () -> {
        Environment mockEnv = mock(Environment.class);
        new LoggingConfig(mockEnv).init();
      }
    );

    // jsonLogging is false
    assertDoesNotThrow(
      () -> {
        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getProperty(anyString())).thenReturn("false");
        new LoggingConfig(mockEnv).init();
      }
    );

    // jsonLogging is true
    assertDoesNotThrow(
      () -> {
        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getProperty(anyString())).thenReturn("true");
        new LoggingConfig(mockEnv).init();
      }
    );
  }

}
