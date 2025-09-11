package gov.cms.ab2d.properties.client;

import kong.unirest.GenericType;
import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.Unirest;
import lombok.Getter;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class PropertiesClientImpl implements PropertiesClient {
    @Getter
    private String url = "http://localhost:8060";

    @Getter
    private String configFileName = "application.properties";

    private static final String JSON = "application/json";
    private static final String ACCEPT = "accept";

    public PropertiesClientImpl(String url) {
        this.url = url;
    }

    public PropertiesClientImpl() {
        String envUrl = getFromEnvironment();
        if (StringUtils.isNotEmpty(envUrl)) {
            url = envUrl;
            return;
        }
        PropertiesConfiguration config = new PropertiesConfiguration();
        try {
            config.load(getConfigFileName());
            String configUrl = config.getString("properties.service.url");
            if (StringUtils.isNotEmpty(configUrl)) {
                url = configUrl;
            }
        } catch (Exception ex) {
            // No need to do anything, it will end up using the default
        }
    }

    public String getFromEnvironment() {
        return System.getenv("PROPERTIES_SERVICE_URL");
    }

    @Override
    public List<Property> getAllProperties() {
        try {
            HttpResponse<List<Property>> response = Unirest.get(url + "/properties")
                    .header(ACCEPT, JSON)
                    .asObject(new GenericType<List<Property>>() {
                    });
            List<Property> values = response.getBody();
            if (values == null) {
                throw new PropertyNotFoundException("Cannot find the list of properties");
            }
            return values;
        } catch (Exception ex) {
            throw new PropertyNotFoundException("Cannot find the list of properties", ex);
        }
    }

    @Override
    public Property getProperty(String key) {
        try {
            HttpResponse<Property> propResponse = Unirest.get(url + "/properties/" + key).header(ACCEPT, JSON)
                    .asObject(new GenericType<>() {
                    });
            if (propResponse.getStatus() == HttpStatus.NOT_FOUND) {
                throw new PropertyNotFoundException("Property " + key + " is not defined");
            }
            Property prop = propResponse.getBody();
            if (prop == null || prop.getKey() == null || prop.getKey().equalsIgnoreCase("null")) {
                throw new PropertyNotFoundException("Property " + key + " is not defined");
            }
            return prop;
        } catch (Exception ex) {
            throw new PropertyNotFoundException("Cannot find the property " + key, ex);
        }
    }

    @Override
    public Property setProperty(String key, String value) {
        try {
            HttpResponse<Property> response = Unirest.post(url + "/properties").header(ACCEPT, JSON)
                    .field("key", key)
                    .field("value", value)
                    .asObject(new GenericType<Property>() {
                    });
            return response.getBody();
        } catch (Exception ex) {
            throw new PropertyNotFoundException("Cannot save the property " + key, ex);
        }
    }

    @Override
    public void deleteProperty(String key) {
        try {
            HttpResponse<String> result = Unirest.delete(url + "/properties/" + key).asString();
            if (result == null || result.getBody() == null || !result.getBody().equalsIgnoreCase("true")) {
                throw new PropertyNotFoundException("Unable to delete " + key);
            }
        } catch (Exception ex) {
            throw new PropertyNotFoundException("Cannot delete the property " + key, ex);
        }
    }
}
