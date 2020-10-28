package gov.cms.ab2d.common.service;

public enum FeatureEngagement {
    IN_GEAR("engaged"), // default - feature is fully contributing
    NEUTRAL("idle");  // out of gear - feature is not in play

    private String serialValue;

    FeatureEngagement(String propValue) {
        serialValue = propValue;
    }

    public String getSerialValue() {
        return serialValue;
    }

    // Defaults to being IN_GEAR.
    public static FeatureEngagement fromString(String rawValue) {
        return "idle".equals(rawValue) ? NEUTRAL : IN_GEAR;
    }
}
