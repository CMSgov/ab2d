package gov.cms.ab2d.worker.processor.prototype;

/**
 * How a worker came to own a job. Recorded as a tag on the pause/resume metrics so that a fresh start,
 * a clean pause that was picked back up, and a crash that had to be healed are all countable separately.
 */
public enum ResumeMode {

    /** Nobody has ever held this job, so there is no prior work to resume. */
    FRESH,

    /** The prior owner suspended cleanly, so the same fence token and output files are adopted as-is. */
    SOFT,

    /** The prior owner died or was superseded, so the token was bumped and the batch state healed. */
    HARD;

    /** Lowercase name, used as the metric tag value. */
    public String tagValue() {
        return name().toLowerCase();
    }
}
