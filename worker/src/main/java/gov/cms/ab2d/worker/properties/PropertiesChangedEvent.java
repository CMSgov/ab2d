package gov.cms.ab2d.worker.properties;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class PropertiesChangedEvent extends ApplicationEvent {

    @Getter
    private Map<String, Object> propertiesMap;

    // An event that gets fired by the PropertiesChangeDetection
    public PropertiesChangedEvent(Object source, Map<String, Object> propertiesMap) {
        super(source);
        this.propertiesMap = propertiesMap;
    }
}
