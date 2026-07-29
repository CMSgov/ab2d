package gov.cms.ab2d.worker.processor.prototype.lease;

/**
 * Thrown when a worker tries to make a change to the DB but its token is expired.
 * Should only happen in the rare case when a worker misses its heartbeat multiple times.
 */
public class FenceLostException extends RuntimeException {

    public FenceLostException(String jobUuid, long heldToken) {
        super("lease lost for job " + jobUuid + ": held token " + heldToken
                + " is no longer the current lease token",
                null, false, false);
    }
}
