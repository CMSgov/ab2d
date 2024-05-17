package gov.cms.ab2d.api.controller.common;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

public class Common {
    private static final String HTTPS_STRING = "https";

    public static boolean shouldReplaceWithHttps(HttpServletRequest request) {
        return HTTPS_STRING.equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    public static String getCurrentUrl(HttpServletRequest request) {
        return Common.shouldReplaceWithHttps(request) ?
                ServletUriComponentsBuilder.fromCurrentRequest().scheme(HTTPS_STRING).toUriString() :
                ServletUriComponentsBuilder.fromCurrentRequest().toUriString().replace(":80/", "/");
    }

    public static String getUrl(String ending, HttpServletRequest request) {
        return Common.shouldReplaceWithHttps(request) ?
                ServletUriComponentsBuilder.fromCurrentRequestUri().scheme(HTTPS_STRING).replacePath(ending).toUriString() :
                ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(ending).toUriString().replace(":80/", "/");
    }
}
