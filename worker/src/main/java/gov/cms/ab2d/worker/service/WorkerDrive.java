package gov.cms.ab2d.worker.service;

public enum WorkerDrive {
    IN_GEAR, // default - worker is ready to process jobs
    NEUTRAL;  // out of gear in order to allow for something else

    // Defaults to being IN_GEAR.
    public static WorkerDrive fromString(String rawValue) {
        return "idle".equals(rawValue) ? NEUTRAL : IN_GEAR;
    }
}
