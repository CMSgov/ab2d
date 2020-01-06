package gov.cms.ab2d.common.config;

import gov.cms.ab2d.common.dto.SponsorDTO;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.service.RoleService;
import gov.cms.ab2d.common.service.SponsorService;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

@Component
public class Mapping {

    @Autowired
    private SponsorService sponsorService;

    @Autowired
    private RoleService roleService;

    private ModelMapper modelMapper;

    @PostConstruct
    public void init() {
        modelMapper = new ModelMapper();
        Converter<String, Role> roleDTOToRoleConverter = context -> roleService.findRoleByName(context.getSource());
        Converter<Set<Role>, String> roleToRoleDTOConverter = context -> context.getSource().iterator().next().getName();
        Converter<Sponsor, SponsorDTO> sponsorSponsorDTOConverter = context -> new SponsorDTO(context.getSource().getHpmsId(), context.getSource().getOrgName());
        Converter<SponsorDTO, Sponsor> sponsorDTOSponsorConverter = new AbstractConverter<>() {
            protected Sponsor convert(SponsorDTO source) {
                return sponsorService.findByHpmsIdAndOrgName(source.getHpmsId(), source.getOrgName());
            }
        };

        modelMapper.addConverter(sponsorDTOSponsorConverter);
        modelMapper.createTypeMap(User.class, UserDTO.class)
                .addMappings(mapper -> mapper.using(sponsorSponsorDTOConverter).map(src -> src.getSponsor(), UserDTO::setSponsor))
                .addMappings(mapper -> mapper.using(roleToRoleDTOConverter).map(src -> src.getRoles(), UserDTO::setRole));
        modelMapper.createTypeMap(UserDTO.class, User.class)
                .addMappings(mapper -> mapper.using(roleDTOToRoleConverter).map(src -> src.getRole(), User::addRole));
    }

    public ModelMapper getModelMapper() {
        return modelMapper;
    }
}
