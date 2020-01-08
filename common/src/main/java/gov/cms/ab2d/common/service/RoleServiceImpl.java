package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    public Role findRoleByName(String name) {
        return roleRepository.findRoleByName(name).orElseThrow(() -> {
            log.error("Unable to find role with name {}", name);
            return new ResourceNotFoundException("Unable to find role with name: " + name);
        });
    }
}
