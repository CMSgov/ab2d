package gov.cms.ab2d.eventclient.events;

import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Describes the transition of a job between two statuses
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class JobStatusChangeEvent extends LoggableEvent {
    // The old state
    private String oldStatus;
    // The new state
    private String newStatus;
    // A description if needed whey this state occurred if it isn't obvious
    private String description;

    public JobStatusChangeEvent() { }

    public JobStatusChangeEvent(String organization, String jobId, String oldStatus, String newStatus, String description) {
        super(OffsetDateTime.now(), organization, jobId);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.description = description;
    }

    @Override
    public String asMessage() {

        // Prettify alert
        String label = "";
        if (description != null && !description.isBlank()) {
            String[] labelAndDescription = description.split("\\s+", 2);

            // Single word description then do not apply a label
            if (labelAndDescription.length == 1) {
                description = labelAndDescription[0];
            } else {
                label = labelAndDescription[0];
                description = labelAndDescription[1];
            }
        }

        return String.format("%s (%s) %s -> %s %s", label, getJobId(), oldStatus, newStatus, description);
    }
}
