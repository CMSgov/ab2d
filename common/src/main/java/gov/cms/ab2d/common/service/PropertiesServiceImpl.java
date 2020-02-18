package gov.cms.ab2d.common.service;

import com.google.common.reflect.TypeToken;
import gov.cms.ab2d.common.config.Mapping;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Properties;
import gov.cms.ab2d.common.repository.PropertiesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.List;

@Service
@Transactional
@Slf4j
public class PropertiesServiceImpl implements PropertiesService {

    @Autowired
    private Mapping mapping;

    @Autowired
    private PropertiesRepository propertiesRepository;

    private final Type propertiesListType = new TypeToken<List<PropertiesDTO>>(){}.getType();

    public List<Properties> getAllProperties() {
        return propertiesRepository.findAll();
    }

    public List<PropertiesDTO> getAllPropertiesDTO() {
        List<Properties> properties = getAllProperties();
        return mapping.getModelMapper().map(properties, propertiesListType);
    }
}
