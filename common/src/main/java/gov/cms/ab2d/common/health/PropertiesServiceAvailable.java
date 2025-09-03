package gov.cms.ab2d.common.health;

import gov.cms.ab2d.common.properties.PropertiesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;

@Service
@AllArgsConstructor
@Slf4j
public class PropertiesServiceAvailable {
    private final PropertiesService propertiesService;

    public boolean isAvailable(boolean productionEnv) {
        if (!productionEnv) {
            return true;
        }

        String maintMode = propertiesService.getProperty(MAINTENANCE_MODE, null);
        if (!validBoolean(maintMode)) {
            log.error("Unable to retrieve a valid maintenance value: " + maintMode);
            return false;
        }

        return true;
    }

    public boolean validBoolean(String val) {
        if (StringUtils.isEmpty(val)) {
            return false;
        }
        return val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false");
    }

    public String getValue(String key, String defaultVal) {
        try {
            return propertiesService.getProperty(key, defaultVal);
        } catch (Exception ex) {
            return null;
        }
    }


}
