package gov.cms.ab2d.worker.processor.prototype;

/**
 * Thrown when a job that has already run is picked back up and the aggregated attribution table it was
 * partitioned from is gone.
 */
public class AttributionSnapshotLostException extends RuntimeException {

    public AttributionSnapshotLostException(String jobUuid, String contractNumber) {
        super("job " + jobUuid + " cannot resume because the aggregated attribution table for contract "
                + contractNumber + " no longer exists");
    }
}
