package gov.cms.ab2d.worker.properties;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class PropertiesChangedEvent extends ApplicationEvent {

    private Map<String, Object> propertiesMap;

    public PropertiesChangedEvent(Object source, Map<String, Object> propertiesMap) {
        super(source);
        this.propertiesMap = propertiesMap;
    }
    public Map<String, Object> getPropertiesMap() {
        return propertiesMap;
    }
}
