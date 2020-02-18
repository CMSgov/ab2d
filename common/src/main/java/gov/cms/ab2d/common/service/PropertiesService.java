package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Properties;

import java.util.List;

public interface PropertiesService {

    String getProperty(String key);

    List<Properties> getAllProperties();
}
