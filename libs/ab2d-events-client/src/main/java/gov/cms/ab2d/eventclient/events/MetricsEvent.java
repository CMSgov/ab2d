package gov.cms.ab2d.eventclient.events;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.Objects;


/**
 * Metrics Events occur whe the health of our applications and services change.
 * Example: An API instance loses all connections to out database.
 * A metric event should be thrown when the change is initially detected and ideally when the issue is resolved.
 */
@Data
@NoArgsConstructor
public class MetricsEvent extends LoggableEvent {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetricsEvent)) return false;
        if (!super.equals(o)) return false;
        MetricsEvent that = (MetricsEvent) o;
        return service.equals(that.service)
                && stateType.equals(that.stateType)
                && eventDescription.equals(that.eventDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), service, stateType);
    }

    @Builder
    public MetricsEvent(@NotNull String service, @NotNull String eventDescription, @NotNull OffsetDateTime timeOfEvent, @NotNull State stateType) {
        super();
        super.setTimeOfEvent(timeOfEvent);
        this.service = service;
        this.eventDescription = eventDescription;
        this.stateType = stateType;
    }

    @Override
    public String asMessage() {
        return String.format("(%s) %s", service, stateType);
    }

    @NonNull
    private String service;

    @NonNull
    private String eventDescription;

    @NonNull
    private State stateType;

    public enum State {
        //use start and stop for services that track those states for us. For example cloudwatch.
        START, END,
        // Otherwise, use continue. We can infer rough start and stop times
        CONTINUE;
    }

}
