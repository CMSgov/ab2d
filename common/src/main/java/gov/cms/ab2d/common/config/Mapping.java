package gov.cms.ab2d.common.config;

import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.service.RoleService;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

@Component
public class Mapping {

    @Autowired
    private RoleService roleService;

    private ModelMapper modelMapper;

    @PostConstruct
    public void init() {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setSkipNullEnabled(true);

        Converter<String, Role> roleDTOToRoleConverter = context -> {
            if (context.getSource() == null) {
                return null;
            } else {
                return roleService.findRoleByName(context.getSource());
            }
        };
        Converter<Set<Role>, String> roleToRoleDTOConverter = context -> {
            if (context.getSource() == null || context.getSource().isEmpty()) {
                return null;
            } else {
                return context.getSource().iterator().next().getName();
            }
        };

        modelMapper.createTypeMap(User.class, UserDTO.class)
                .addMappings(mapper -> mapper.using(roleToRoleDTOConverter).map(User::getRoles, UserDTO::setRole))
                .addMappings(mapper -> mapper.map(User::getContract, UserDTO::setContract));
        modelMapper.createTypeMap(UserDTO.class, User.class)
                .addMappings(mapper -> mapper.using(roleDTOToRoleConverter).map(UserDTO::getRole, User::addRole));
    }

    public ModelMapper getModelMapper() {
        return modelMapper;
    }
}
