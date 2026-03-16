package gov.cms.ab2d.common.health;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.List;

/**
 * Does health checks on URLs
 */
@Slf4j
public final class UrlAvailable {

    private UrlAvailable() { }

    /**
     * Given a URL, test to see if you can open a socket to that domain on the specified or default port
     *
     * @param testUrl - The URL to test
     * @return true if you can open a socket to that port on that server
     */
    public static boolean available(String testUrl) {
        try {
            URL url = new URL(testUrl);
            int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
            try (Socket socket = new Socket(url.getHost(), port)) {
                log.debug("Socket opened to " + testUrl + " and is connected: " + socket.isConnected());
            } catch (IOException e) {
                log.info("Unable to open socket to " + testUrl);
                return false;
            }
            return true;
        } catch (MalformedURLException e1) {
            log.info("Malformed URL: " + testUrl);
            return false;
        }
    }

    /**
     * Given a list of URLs, make sure at least one is available
     *
     * @param testUrls - the list of URLs
     * @return true if at least one of them is available
     */
    public static boolean isAnyAvailable(List<String> testUrls) {
        for (String url : testUrls) {
            if (available(url)) {
                return true;
            }
        }

        log.info("No URLs available");

        return false;
    }
}
