package gov.cms.ab2d.eventlogger;

import java.time.OffsetDateTime;

/**
 * Interface describing a loggable event
 */
public interface LoggableEvent {
    boolean log(OffsetDateTime eventTime);
}
