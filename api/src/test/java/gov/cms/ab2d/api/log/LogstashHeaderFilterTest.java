package gov.cms.ab2d.api.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

class LogstashHeaderFilterTest {

  @Test
  void testAddRemoveInclude() {
    LogstashHeaderFilter logstashHeaderFilter = new LogstashHeaderFilter();
    logstashHeaderFilter.addInclude("cats");
    logstashHeaderFilter.addInclude("dogs");
    assertEquals(
      logstashHeaderFilter.getIncludes(),
      new HashSet<>(Arrays.asList("cats", "dogs"))
    );
  }

  @Test
  void testAddRemoveExclude() {
    LogstashHeaderFilter logstashHeaderFilter = new LogstashHeaderFilter();
    logstashHeaderFilter.addExclude("lizards");
    logstashHeaderFilter.addExclude("snakes");
    assertEquals(
      logstashHeaderFilter.getExcludes(),
      new HashSet<>(Arrays.asList("lizards", "snakes"))
    );
  }

  @Test
  void testIncludeHeader1() {
    LogstashHeaderFilter logstashHeaderFilter = new LogstashHeaderFilter();
    assertTrue(logstashHeaderFilter.includeHeader("key", "value"));
  }

  @Test
  void testIncludeHeader2() {
    LogstashHeaderFilter logstashHeaderFilter = new LogstashHeaderFilter();
    logstashHeaderFilter.addInclude("key");
    assertTrue(logstashHeaderFilter.includeHeader("key", "value"));
  }

  @Test
  void testIncludeHeader3() {
    LogstashHeaderFilter logstashHeaderFilter = new LogstashHeaderFilter();
    logstashHeaderFilter.addExclude("key");
    assertFalse(logstashHeaderFilter.includeHeader("key", "value"));
  }

  @Test
  void testIncludeHeader4() {
    LogstashHeaderFilter logstashHeaderFilter = new LogstashHeaderFilter();
    logstashHeaderFilter.addInclude("key");
    logstashHeaderFilter.addExclude("key");
    assertThrows(
      IllegalStateException.class,
      () -> {
        logstashHeaderFilter.includeHeader("key", "value");
      }
    );
  }

}
