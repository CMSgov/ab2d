package gov.cms.ab2d.worker.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
class PropertiesChangeDetection {

    @Autowired
    private PropertiesInit propertiesInit;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    // Every 20 seconds
    @Scheduled(fixedDelay = 20 * 1000)
    public void detectChanges() {
        Map<String, Object> properties = propertiesInit.updatePropertiesFromDatabase();

        PropertiesChangedEvent propertiesChangedEvent = new PropertiesChangedEvent(this, properties);

        // All places that rely on properties changing dynamically from the db will need to listen to this event
        applicationEventPublisher.publishEvent(propertiesChangedEvent);
    }
}
