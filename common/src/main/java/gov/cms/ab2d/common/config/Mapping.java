package gov.cms.ab2d.common.config;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.RoleService;
import org.modelmapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

@Component
public class Mapping {

    @Autowired
    private ContractService contractService;

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

        Converter<Contract, ContractDTO> contractContractDTOConverter = context ->
                new ContractDTO(context.getSource().getContractNumber(), context.getSource().getContractName(),
                                context.getSource().getAttestedOn().toString());
        Converter<ContractDTO, Contract> sponsorDTOSponsorConverter = new AbstractConverter<>() {
            protected Contract convert(ContractDTO source) {
                //noinspection OptionalGetWithoutIsPresent
                return contractService.getContractByContractNumber(source.getContractNumber()).get();
            }
        };

        modelMapper.addConverter(sponsorDTOSponsorConverter);
        modelMapper.createTypeMap(User.class, UserDTO.class)
                .addMappings(mapper -> mapper.using(contractContractDTOConverter).map(User::getContract, UserDTO::setContract))
                .addMappings(mapper -> mapper.using(roleToRoleDTOConverter).map(User::getRoles, UserDTO::setRole))
                .addMappings(mapper -> mapper.map(User::getContract, UserDTO::setContract));
        modelMapper.createTypeMap(UserDTO.class, User.class)
                .addMappings(mapper -> mapper.using(roleDTOToRoleConverter).map(UserDTO::getRole, User::addRole));
    }

    public ModelMapper getModelMapper() {
        return modelMapper;
    }
}
