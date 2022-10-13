package gov.cms.ab2d.properties.utils;

import org.apache.commons.lang3.StringUtils;

public final class Validation {
    private Validation() { }

    public static boolean validInteger(String val) {
        try {
            Long.parseLong(val);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean validBoolean(String val) {
        if (StringUtils.isEmpty(val)) {
            return false;
        }
        return val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false");
    }

    public static boolean validFloat(String val) {
        if (StringUtils.isEmpty(val)) {
            return false;
        }
        try {
            Double.parseDouble(val);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
