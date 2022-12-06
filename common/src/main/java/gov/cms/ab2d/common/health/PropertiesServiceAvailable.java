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

    public boolean isAvailable() {
        String maintMode = propertiesService.getProperty(MAINTENANCE_MODE, "false");
        if (!validBoolean(maintMode)) {
            log.error("Unable to retrieve a valid maintenance value: " + maintMode);
            return false;
        }

        String fakeKey = "fake.key";
        String fakeValue = "fake_value";

        // Create a fake key
        boolean canCreate = createValue(fakeKey, fakeValue);
        if (!canCreate) {
            log.error("Cannot create a new property");
            return false;
        }

        // Update fake value
        boolean canUpdate = updateValue(fakeKey, fakeValue + "1");
        if (!canUpdate) {
            log.error("Cannot update a property");
            return false;
        }

        // Delete property
        boolean canDelete = deleteValue(fakeKey);
        if (!canDelete) {
            log.error("Cannot delete a property");
            return false;
        }
        log.info("Properties Service is Available");
        return true;
    }

    boolean createValue(String key, String value) {
        if (!propertiesService.createProperty(key, value)) {
            log.error("Unable to create a new property");
            return false;
        }
        return checkValue(key, value);
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

    public boolean checkValue(String key, String value) {
        String gottenValue = getValue(key, "!!!");
        if (value == null && gottenValue == null) {
            return true;
        }
        if (value == null || gottenValue == null) {
            return false;
        }
        return value.equalsIgnoreCase(gottenValue);
    }

    public boolean deleteValue(String key) {
        boolean canDelete = propertiesService.deleteProperty(key);
        if (!canDelete) {
            log.error("Unable to get delete new property");
            return false;
        }
        try {
            String newProp2 = propertiesService.getProperty(key, null);
            if (newProp2 != null) {
                log.error("Unable to get delete new property");
                return false;
            }
        } catch (Exception ex) {
            return true;
        }
        return true;
    }

    boolean updateValue(String key, String value) {
        // Update fake value
        boolean canUpdate = propertiesService.updateProperty(key, value);
        if (!canUpdate) {
            log.error("Unable to get update new property");
            return false;
        }
        return checkValue(key, value);
    }
}