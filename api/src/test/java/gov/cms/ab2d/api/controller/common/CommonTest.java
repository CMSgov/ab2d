package gov.cms.ab2d.api.controller.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class CommonTest {

  @Test
  void testShouldReplaceWithHttps() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    assertFalse(Common.shouldReplaceWithHttps(req));
  }

  @Test
  void testGetCurrentUrl() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    req.setMethod("GET");
    req.setScheme("http");
    req.setServerName("ab2d.cms.gov");
    req.setServerPort(1234);
    req.setContextPath("/v1/fhir");
    req.setServletPath("/job/123");
    req.setQueryString("a=1&b=2");
    assertEquals("http://ab2d.cms.gov:1234?a=1&b=2", Common.getCurrentUrl(req));

    req.addHeader("X-Forwarded-Proto", "https");
    req.addHeader("X-Forwarded-Port", "443");
    assertEquals("https://ab2d.cms.gov:1234?a=1&b=2", Common.getCurrentUrl(req));
  }

  @Test
  void testGetUrl() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    req.setMethod("GET");
    req.setScheme("http");
    req.setServerName("ab2d.cms.gov");
    req.setServerPort(1234);
    req.setContextPath("/v1/fhir");
    req.setServletPath("/job/123");
    req.setQueryString("a=1&b=2");
    assertEquals("http://ab2d.cms.gov:1234/ending", Common.getUrl("ending", req));

    req.addHeader("X-Forwarded-Proto", "https");
    req.addHeader("X-Forwarded-Port", "443");
    assertEquals("https://ab2d.cms.gov:1234/ending", Common.getUrl("ending", req));
  }

}
