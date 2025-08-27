package gov.cms.ab2d.eventlogger.utils;

import gov.cms.ab2d.eventclient.events.LoggableEvent;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public final class UtilMethods {

    private UtilMethods() { }

    public static LocalDateTime convertToUtc(OffsetDateTime dt) {
        if (dt != null) {
            // Move it to UTC since that is what it's stored as
            return dt.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } else {
            return null;
        }
    }

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

    public static String hashIt(String val) {
        if (val == null) {
            return null;
        }
        return Hex.encodeHexString(DigestUtils.sha256(val));
    }

    public static String hashIt(InputStream stream) throws IOException {
        return Hex.encodeHexString(DigestUtils.sha256(stream));
    }

    /**
     * Detect if a Loggable event contains an Okta client id
     */
    public static boolean containsClientId(LoggableEvent event) {

        String organization = event.getOrganization();
        if (StringUtils.isBlank(organization)) {
            return false;
        }

        return organization.trim().startsWith("0");
    }

    public static boolean allEmpty(List<?>... events) {
        for (List<?> e : events) {
            if (e != null && !e.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static String camelCaseToUnderscore(String val) {
        return val.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
    }
}
