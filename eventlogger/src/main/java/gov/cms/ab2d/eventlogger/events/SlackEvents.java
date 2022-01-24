package gov.cms.ab2d.eventlogger.events;

/**
 * All slack events that AB2D reports. These events may be reported to
 */
public enum SlackEvents {

    // Call to AB2D API contains an error
    API_AUTHNZ_ERROR,
    API_INVALID_CONTRACT,

    // HPMS related alerts
    CONTRACT_ADDED,
    CONTRACT_CHANGED,

    // Enrollment update issues
    COVERAGE_DELETE_FAILED,
    COVERAGE_UPDATE_FAILED,
    COVERAGE_UPDATES_FAILED,
    COVERAGE_VERIFICATION_ABORTED,
    COVERAGE_VERIFICATION_FAILURE,


    // EOB job related issues
    EOB_JOB_CALL_FAILURE,
    EOB_JOB_CANCELLED,
    EOB_JOB_COMPLETED,
    EOB_JOB_COVERAGE_ISSUE,
    EOB_JOB_FAILURE,
    EOB_JOB_QUEUE_MISMATCH,
    EOB_JOB_RESUBMITTED,
    EOB_JOB_STARTED,

    // Maintenance mode
    MAINT_MODE,

    // First time an organization is running a job
    ORG_FIRST
}
