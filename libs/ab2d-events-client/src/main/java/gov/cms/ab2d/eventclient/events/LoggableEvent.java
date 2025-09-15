package gov.cms.ab2d.eventclient.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import java.time.OffsetDateTime;

import lombok.Data;

/**
 * Interface describing a loggable event
 */
@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_ARRAY
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ApiRequestEvent.class, name = "ApiRequestEvent"),
        @JsonSubTypes.Type(value = ApiResponseEvent.class, name = "ApiResponseEvent"),
        @JsonSubTypes.Type(value = BeneficiarySearchEvent.class, name = "BeneficiarySearchEvent"),
        @JsonSubTypes.Type(value = ContractSearchEvent.class, name = "ContractSearchEvent"),
        @JsonSubTypes.Type(value = ErrorEvent.class, name = "ErrorEvent"),
        @JsonSubTypes.Type(value = FileEvent.class, name = "FileEvent"),
        @JsonSubTypes.Type(value = JobStatusChangeEvent.class, name = "JobStatusChangeEvent"),
        @JsonSubTypes.Type(value = JobSummaryEvent.class, name = "JobSummaryEvent"),
        @JsonSubTypes.Type(value = MetricsEvent.class, name = "MetricsEvent"),
        @JsonSubTypes.Type(value = ReloadEvent.class, name = "ReloadEvent"),
        @JsonSubTypes.Type(value = SlackEvents.class, name = "SlackEvents"),
})
public abstract class LoggableEvent {
    protected LoggableEvent() { }

    // If it's dev, prod, etc.
    private String environment;

    // DB id if there is one
    private Long id;

    // AWS Id if there is one
    private String awsId;

    // Time the event occurred
    private OffsetDateTime timeOfEvent;

    // The organization the event may be related to
    // must not contain a secret value
    private String organization;

    // The job the event may be related to
    private String jobId;

    /**
     * A loggable event with the minimum fields necessary for logging
     * @param timeOfEvent time when the event occurs
     * @param organization name of the organization (not the credential) the event is associated with
     * @param jobId uuid of job
     * @throws IllegalArgumentException if the organization may be an okta client credential
     */
    protected LoggableEvent(OffsetDateTime timeOfEvent, String organization, String jobId) {
        this.timeOfEvent = timeOfEvent;
        this.organization = organization;
        this.jobId = jobId;
    }

    public void setEnvironment(Ab2dEnvironment executionEnv) {
        this.environment = executionEnv.getName();
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * Convert event to a message
     * @return the event converted to an alert message.
     */
    public abstract String asMessage();

    /**
     * If you implement equals, you have to do hashCode. I gook the one created by lombok and cleaned it.
     * @return the hash code
     */
    public int hashCode() {
        int result = 1;
        result = result * 59 + (environment == null ? 43 : environment.hashCode());
        result = result * 59 + (id == null ? 43 : id.hashCode());
        result = result * 59 + (awsId == null ? 43 : awsId.hashCode());
        result = result * 59 + (timeOfEvent == null ? 43 : timeOfEvent.hashCode());
        result = result * 59 + (organization == null ? 43 : organization.hashCode());
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
        } else if (otherTimeOfEvent == null || thisTimeOfEvent.toEpochSecond() != otherTimeOfEvent.toEpochSecond()) {
            return false;
        }

        String thisUser = this.getOrganization();
        String otherUser = other.getOrganization();
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
