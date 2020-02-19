package gov.cms.ab2d.worker.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
class PropertiesChangeDetection {

    @Autowired
    private PropertiesInit propertiesInit;

    // Every 10 minutes
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void detectChanges() {
        propertiesInit.updatePropertiesFromDatabase();
    }
}
