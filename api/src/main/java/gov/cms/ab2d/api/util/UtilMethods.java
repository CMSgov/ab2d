package gov.cms.ab2d.api.util;

import jakarta.servlet.http.HttpServletRequest;

public final class UtilMethods {

    private UtilMethods() { }

    public static String parseJobId(String requestUri) {
        if (requestUri == null) {
            return null;
        }
        if (requestUri.contains("/Job/")) {
            String firstPart = requestUri.substring(requestUri.indexOf("/Job/") + 5);
            if (firstPart.indexOf('/') < 0) {
                return null;
            }
            return firstPart.substring(0, firstPart.indexOf("/"));
        }
        return null;
    }

    public static String getURL(HttpServletRequest req) {
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

    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        // If behind load balancer, get the real IP
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff;
        }
        return ipAddress;
    }
}
