package gov.cms.ab2d.eventlogger.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class UtilMethodsTest {

    @Test
    void getJobId() {
        assertNull(UtilMethods.parseJobId(null));
        assertNull(UtilMethods.parseJobId("http://localhost:8080/abc"));
        assertEquals("123", UtilMethods.parseJobId("http://localhost:8080/Job/123/abc"));
        assertEquals("123", UtilMethods.parseJobId("http://localhost:8080/Job/123/$status"));
        assertEquals("123", UtilMethods.parseJobId("http://localhost:8080//Job/123//file/file1"));
    }

    @Test
    void getUrl() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("GET");
        req.setScheme("http");
        req.setServerName("ab2d.cms.gov");     // hostname.com
        req.setServerPort(1234);
        req.setContextPath("/v1/fhir");   // /mywebapp
        req.setServletPath("/job/123");   // /servlet/MyServlet
        req.setQueryString("a=1&b=2");   // d=789

        assertEquals("GET http://ab2d.cms.gov:1234/v1/fhir/job/123?a=1&b=2", UtilMethods.getURL(req));
        req.addHeader("X-Forwarded-Proto", "https");
        req.addHeader("X-Forwarded-Port", "443");

        assertEquals("GET https://ab2d.cms.gov/v1/fhir/job/123?a=1&b=2", UtilMethods.getURL(req));
    }

    @Test
    void getIpAddress() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("1.2.3.4");
        assertEquals("1.2.3.4", UtilMethods.getIpAddress(req));
        req.addHeader("X-Forwarded-For", "2.3.4.5");
        assertEquals("2.3.4.5", UtilMethods.getIpAddress(req));
    }
}