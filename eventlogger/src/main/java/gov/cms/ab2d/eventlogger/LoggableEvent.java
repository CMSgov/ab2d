package gov.cms.ab2d.eventlogger;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Interface describing a loggable event
 */
@Data
public abstract class LoggableEvent {
    public LoggableEvent() { }

    // If it's dev, prod, etc.
    private String environment;

    // DB id if there is one
    private Long id;

    // AWS Id if there is one
    private String awsId;

    // Time the event occurred
    private OffsetDateTime timeOfEvent;

    // The user the event may be related to
    private String user;

    // The job the event may be related to
    private String jobId;

    public LoggableEvent(OffsetDateTime timeOfEvent, String user, String jobId) {
        this.timeOfEvent = timeOfEvent;
        this.user = user;
        this.jobId = jobId;
    }

    /**
     * If you implement equals, you have to do hashCode. I gook the one created by lombok and cleaned it.
     * @return the hash code
     */
    public int hashCode() {
        int result = 1;
        String environment = this.getEnvironment();
        result = result * 59 + (environment == null ? 43 : environment.hashCode());
        Long id = this.getId();
        result = result * 59 + (id == null ? 43 : id.hashCode());
        String awsId = this.getAwsId();
        result = result * 59 + (awsId == null ? 43 : awsId.hashCode());
        OffsetDateTime timeOfEvent = this.getTimeOfEvent();
        result = result * 59 + (timeOfEvent == null ? 43 : timeOfEvent.hashCode());
        String user = this.getUser();
        result = result * 59 + (user == null ? 43 : user.hashCode());
        String jobId = this.getJobId();
        result = result * 59 + (jobId == null ? 43 : jobId.hashCode());
        return result;
    }

    /**
     * Override this because in our situation equals for time of event shouldn't care about timezone
     *
     * @param o - the other object to care about
     * @return true if they have the same data
     */
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof LoggableEvent)) {
            return false;
        }
        LoggableEvent other = (LoggableEvent) o;
        String thisEnvironment = this.getEnvironment();
        String otherEnvironment = other.getEnvironment();
        if (thisEnvironment == null) {
            if (otherEnvironment != null) {
                return false;
            }
        } else if (!thisEnvironment.equals(otherEnvironment)) {
            return false;
        }

        Long thisId = this.getId();
        Long otherId = other.getId();
        if (thisId == null) {
            if (otherId != null) {
                return false;
            }
        } else if (!thisId.equals(otherId)) {
            return false;
        }

        String thisAwsId = this.getAwsId();
        String otherAwsId = other.getAwsId();
        if (thisAwsId == null) {
            if (otherAwsId != null) {
                return false;
            }
        } else if (!thisAwsId.equals(otherAwsId)) {
            return false;
        }

        OffsetDateTime thisTimeOfEvent = this.getTimeOfEvent();
        OffsetDateTime otherTimeOfEvent = other.getTimeOfEvent();
        if (thisTimeOfEvent == null) {
            if (otherTimeOfEvent != null) {
                return false;
            }
        } else if (thisTimeOfEvent.toEpochSecond() != otherTimeOfEvent.toEpochSecond()) {
            return false;
        }

        String thisUser = this.getUser();
        String otherUser = other.getUser();
        if (thisUser == null) {
        if (otherUser != null) {
                return false;
            }
        } else if (!thisUser.equals(otherUser)) {
            return false;
        }

        String thisJobId = this.getJobId();
        String otherJobId = other.getJobId();
        if (thisJobId == null) {
            return otherJobId == null;
        } else return thisJobId.equals(otherJobId);
    }
}
