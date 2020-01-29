package gov.cms.ab2d.e2etest;

public class JobUtil {

    public static String getJobUuid(String url) {
        return url.substring(url.indexOf("/Job/") + 5, url.indexOf("/$status"));
    }
}
