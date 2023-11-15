package gov.cms.ab2d.api.controller.common;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

public class Common {
    public static boolean shouldReplaceWithHttps(HttpServletRequest request) {
        return "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    public static String getCurrentUrl(HttpServletRequest request) {
        return Common.shouldReplaceWithHttps(request) ?
                ServletUriComponentsBuilder.fromCurrentRequest().scheme("https").toUriString() :
                ServletUriComponentsBuilder.fromCurrentRequest().toUriString().replace(":80/", "/");
    }

    public static String getUrl(String ending, HttpServletRequest request) {
        return Common.shouldReplaceWithHttps(request) ?
                ServletUriComponentsBuilder.fromCurrentRequestUri().scheme("https").replacePath(ending).toUriString() :
                ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(ending).toUriString().replace(":80/", "/");
    }
}
