package gov.cms.ab2d.worker.properties;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@AllArgsConstructor
@ConditionalOnProperty(value = "property.change.detection", havingValue = "true", matchIfMissing = true)
@Component()
class PropertiesChangeDetection {

    private final PropertiesInit propertiesInit;
    private final ApplicationEventPublisher applicationEventPublisher;

    // Every 20 seconds
    @Scheduled(fixedDelay = 20 * 1000)
    public void detectChanges() {
        Map<String, Object> properties = propertiesInit.updatePropertiesFromDatabase();

        PropertiesChangedEvent propertiesChangedEvent = new PropertiesChangedEvent(this, properties);

        // All places that rely on properties changing dynamically from the db will need to listen to this event
        applicationEventPublisher.publishEvent(propertiesChangedEvent);
    }
}
