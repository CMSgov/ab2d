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

        assertEquals("GET http://ab2d.cms.gov:1234/v1/fhir/job/123?a=1&b=2", getURL(req));
        req.addHeader("X-Forwarded-Proto", "https");
        req.addHeader("X-Forwarded-Port", "443");

        assertEquals("GET https://ab2d.cms.gov/v1/fhir/job/123?a=1&b=2", getURL(req));
    }

    @Test
    void getIpAddress() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("1.2.3.4");
        assertEquals("1.2.3.4", getIpAddress(req));
        req.addHeader("X-Forwarded-For", "2.3.4.5");
        assertEquals("2.3.4.5", getIpAddress(req));
    }

    public static String getURL(MockHttpServletRequest req) {
        String method = req.getMethod();             // GET, POST
        String scheme = req.getScheme();             // http
        // If behind load balancer, get the real scheme
        String forwardedSchmed = req.getHeader("X-Forwarded-Proto");
        if (forwardedSchmed != null && !forwardedSchmed.isEmpty()) {
            scheme = forwardedSchmed;
        }
        String serverName = req.getServerName();     // hostname.com
        int serverPort = req.getServerPort();        // 80
        // If behind load balancer, get the real port
        String forwardedPort = req.getHeader("X-Forwarded-Port");
        if (forwardedPort != null && !forwardedPort.isEmpty()) {
            serverPort = Integer.parseInt(forwardedPort);
        }
        String contextPath = req.getContextPath();   // /mywebapp
        String servletPath = req.getServletPath();   // /servlet/MyServlet
        String pathInfo = req.getPathInfo();         // /a/b;c=123
        String queryString = req.getQueryString();   // d=789

        // Reconstruct original requesting URL
        StringBuilder url = new StringBuilder();
        url.append(method).append(" ");
        url.append(scheme).append("://").append(serverName);

        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append(servletPath);

        if (pathInfo != null) {
            url.append(pathInfo);
        }
        if (queryString != null) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    public static String getIpAddress(MockHttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        // If behind load balancer, get the real IP
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff;
        }
        return ipAddress;
    }
}