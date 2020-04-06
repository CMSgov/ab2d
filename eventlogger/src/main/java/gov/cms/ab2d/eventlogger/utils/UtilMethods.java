package gov.cms.ab2d.eventlogger.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
}
