package gov.cms.ab2d.common.properties;

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
     * Determine if the value of the property is true
     *
     * @param toggleName - the property key
     * @param defaultValue - the default value if it can't be found
     * @return true if the value is true, false otherwise
     */
    boolean isToggleOn(String toggleName, boolean defaultValue);

}
