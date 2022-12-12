package gov.cms.ab2d.common.properties;

import java.util.List;

public interface PropertiesService {
    /**
     * Retrieve the value of a property
     *
     * @param property - the property key
     * @param defaultValue - the default value if it can't be found
     * @return - the property value
     */
    String getProperty(String property, String defaultValue);

    /**
     * Update the value of an existing property
     *
     * @param property - the property key
     * @param value - the new value
     * @return true if the update was successful
     */
    boolean updateProperty(String property, String value);

    /**
     * Retrieve all known properties and their values
     *
     * @return the list of properties
     */
    List<PropertiesDTO> getAllProperties();

    /**
     * Determine if the value of the property is true
     *
     * @param toggleName - the property key
     * @param defaultValue - the default value if it can't be found
     * @return true if the value is true, false otherwise
     */
    boolean isToggleOn(String toggleName, boolean defaultValue);

    /**
     * Create a new property
     *
     * @param key - the key name
     * @param value - the value
     * @return true if the creation was successful
     */
    boolean createProperty(String key, String value);

    /**
     * Delete a property and its value
     *
     * @param key - the property key
     * @return true if the deletion was successful
     */
    boolean deleteProperty(String key);
}
