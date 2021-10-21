package gov.cms.ab2d.common.config;

import gov.cms.ab2d.common.dto.PdpClientDTO;
import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.model.*;    // NOPMD
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.RoleService;
import org.modelmapper.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.Set;

@Component
public class Mapping {

    private final ContractService contractService;

    private final RoleService roleService;

    private ModelMapper modelMapper;

    public Mapping(ContractService contractService, RoleService roleService) {
        this.contractService = contractService;
        this.roleService = roleService;
    }

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
                Optional<Contract> contract = contractService.getContractByContractNumber(source.getContractNumber());
                return contractService.getContractByContractNumber(source.getContractNumber()).get(); //NOSONAR
            }
        };

        modelMapper.addConverter(sponsorDTOSponsorConverter);
        modelMapper.createTypeMap(PdpClient.class, PdpClientDTO.class)
                .addMappings(mapper -> mapper.using(contractContractDTOConverter).map(PdpClient::getContract, PdpClientDTO::setContract))
                .addMappings(mapper -> mapper.using(roleToRoleDTOConverter).map(PdpClient::getRoles, PdpClientDTO::setRole))
                .addMappings(mapper -> mapper.map(PdpClient::getContract, PdpClientDTO::setContract));
        modelMapper.createTypeMap(PdpClientDTO.class, PdpClient.class)
                .addMappings(mapper -> mapper.using(roleDTOToRoleConverter).map(PdpClientDTO::getRole, PdpClient::addRole))
                .addMappings(mapper -> mapper.map(PdpClientDTO::getClientId, PdpClient::setClientId));
    }

    public ModelMapper getModelMapper() {
        return modelMapper;
    }
}
