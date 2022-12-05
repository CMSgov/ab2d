package gov.cms.ab2d.worker;

import gov.cms.ab2d.common.properties.PropertiesDTO;
import gov.cms.ab2d.common.properties.PropertiesService;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyServiceStub implements PropertiesService {
    private Map<String, String> propertiesMap = new HashMap<>();
    static Map<String, String> propertiesMapOrig;
    static {
        propertiesMapOrig = new HashMap<>();

        propertiesMapOrig.put("pcp.max.pool.size", "20");
        propertiesMapOrig.put("pcp.scaleToMax.time", "20");
        propertiesMapOrig.put("pcp.core.pool.size", "3");
        propertiesMapOrig.put("property.change.detection", "false");
        propertiesMapOrig.put("ZipSupportOn", "false");
        propertiesMapOrig.put("worker.engaged", "engaged");
        propertiesMapOrig.put("hpms.ingest.engaged", "engaged");
        propertiesMapOrig.put("coverage.update.stuck.hours", "24");
        propertiesMapOrig.put("coverage.update.discovery", "engaged");
        propertiesMapOrig.put("coverage.update.queueing", "engaged");
        propertiesMapOrig.put("coverage.update.months.past", "1");
        propertiesMapOrig.put("coverage.update.override", "false");
        propertiesMapOrig.put("maintenance.mode", "false");
    }

    public void reset() {
        propertiesMap.clear();
        propertiesMap.putAll(propertiesMapOrig);
    }

    public PropertyServiceStub() {
        reset();
    }

    @Override
    public String getProperty(String property, String defaultValue) {
        return propertiesMap.get(property);
    }

    @Override
    public boolean updateProperty(String property, String value) {
        propertiesMap.put(property, value);
        return true;
    }

    @Override
    public List<PropertiesDTO> getAllProperties() {
        return propertiesMap.entrySet().stream().map(e -> new PropertiesDTO(e.getKey(), e.getValue())).toList();
    }

    @Override
    public boolean isToggleOn(String toggleName, boolean defaultValue) {
        return "true".equalsIgnoreCase(getProperty(toggleName, "" + defaultValue));
    }

    @Override
    public boolean createProperty(String key, String value) {
        propertiesMap.put(key, value);
        return true;
    }

    @Override
    public boolean deleteProperty(String key) {
        propertiesMap.remove(key);
        return true;
    }
}
