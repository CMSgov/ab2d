package gov.cms.ab2d.lambdalibs.lib;

import gov.cms.ab2d.lambdalibs.exceptions.PropertiesException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

    private PropertiesUtil() {

    }

    public static Properties loadProps() {
        return addEnviProps(addSysProps(overrideProps(getProps())));

    }

    private static Properties addEnviProps(Properties props) {
        props.putAll(System.getProperties());
        return props;
    }

    private static Properties addSysProps(Properties props) {
        props.putAll(System.getenv());
        return props;
    }

    private static Properties getProps() {
        java.util.Properties properties = new java.util.Properties();
        try (InputStream is = PropertiesUtil.class.getResourceAsStream("/application.properties")) {
            properties.load(is);
        } catch (IOException e) {
            throw new PropertiesException(e);
        }
        return addEFS(properties);
    }

    private static Properties overrideProps(Properties properties) {
        properties.forEach((key, value) -> {
            String propKey = String.valueOf(key);
            if (System.getProperty(propKey) != null) {
                properties.setProperty(propKey, System.getProperty(propKey));
            }
            if (System.getenv(propKey) != null) {
                properties.setProperty(propKey, System.getenv(propKey));
            }
        });
        return properties;
    }

    private static Properties addEFS(Properties properties) {
        String mount = properties.getProperty("AB2D_EFS_MOUNT");
        properties.put("AB2D_EFS_MOUNT", mount == null ? System.getProperty("java.io.tmpdir") + "/jobdownloads/" : mount);
        return properties;
    }
}
