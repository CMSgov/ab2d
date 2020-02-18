package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.repository.PropertiesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PropertiesServiceImpl implements PropertiesService {

    @Autowired
    private PropertiesRepository propertiesRepository;

    public String getProperty(String key) {
        return propertiesRepository.findByKey(key).map(Properties::getValue).orElseThrow(() -> {
            log.error("No entry was found for key {}", key);
            return new ResourceNotFoundException("No entry was found for key " + key);
        });
    }

    public List<Properties> getAllProperties() {
        return propertiesRepository.findAll();
    }
}
