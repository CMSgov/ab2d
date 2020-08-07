package gov.cms.ab2d.worker.service;

public enum WorkerDrive {
    IN_GEAR("engaged"), // default - worker is ready to process jobs
    NEUTRAL("idle");  // out of gear in order to allow for something else

    private String serialValue;

    WorkerDrive(String propValue) {
        serialValue = propValue;
    }

    public String getSerialValue() {
        return serialValue;
    }

    // Defaults to being IN_GEAR.
    public static WorkerDrive fromString(String rawValue) {
        return "idle".equals(rawValue) ? NEUTRAL : IN_GEAR;
    }
}
