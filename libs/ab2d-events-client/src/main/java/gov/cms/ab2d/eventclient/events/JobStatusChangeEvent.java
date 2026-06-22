package gov.cms.ab2d.eventclient.events;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Describes the transition of a job between two statuses
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class JobStatusChangeEvent extends LoggableEvent {
    private String oldStatus;
    private String newStatus;
    private String description;
    private String contractNumber;
    private OffsetDateTime since;
    private OffsetDateTime until;
    private List<String> serviceDates;
    private String resourceTypes;
    private String version;

    public JobStatusChangeEvent() { }

    public JobStatusChangeEvent(String organization, String jobId, String oldStatus, String newStatus, String description) {
        super(OffsetDateTime.now(), organization, jobId);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.description = description;
    }

    public JobStatusChangeEvent(String organization, String jobId, String oldStatus, String newStatus, String description,
                                String contractNumber, OffsetDateTime since, OffsetDateTime until,
                                List<String> serviceDates, String resourceTypes, String version) {
        this(organization, jobId, oldStatus, newStatus, description);
        this.contractNumber = contractNumber;
        this.since = since;
        this.until = until;
        this.serviceDates = serviceDates;
        this.resourceTypes = resourceTypes;
        this.version = version;
    }

    @Override
    public String asMessage() {
        String label = "";
        String alertMessage = "";
        if (description != null && !description.isBlank()) {
            String[] parts = description.split("\\s+", 2);
            if (parts.length == 2) {
                label = parts[0];
                alertMessage = parts[1];
            } else {
                alertMessage = parts[0];
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(label).append("\n");

        String org = getOrganization();
        if (contractNumber != null || org != null) {
            if (contractNumber != null && org != null) {
                sb.append(contractNumber).append(" - ").append(org);
            } else if (contractNumber != null) {
                sb.append(contractNumber);
            } else {
                sb.append(org);
            }
            sb.append("\n");
        }

        if (version != null && !version.isBlank()) {
            sb.append("Version: ").append(version).append("\n");
        }
        sb.append("Job ID: ").append(getJobId()).append("\n");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        if (since != null) {
            sb.append("Since date: ").append(since.format(formatter)).append("\n");
        }
        if (until != null) {
            sb.append("Until date: ").append(until.format(formatter)).append("\n");
        }
        if (serviceDates != null && !serviceDates.isEmpty()) {
            sb.append("Service date: ");
            if (serviceDates.size() >= 2) {
                sb.append(serviceDates.get(0)).append(" to ").append(serviceDates.get(serviceDates.size() - 1));
            } else {
                sb.append(serviceDates.get(0));
            }
            sb.append("\n");
        }

        sb.append("\n");
        sb.append(oldStatus).append(" -> ").append(newStatus).append(" ").append(alertMessage);

        return sb.toString().stripTrailing();
    }
}
