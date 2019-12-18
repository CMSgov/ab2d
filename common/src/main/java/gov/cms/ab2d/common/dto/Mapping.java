package gov.cms.ab2d.common.dto;

import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.service.SponsorService;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;

public class Mapping {

    private final ModelMapper modelMapper;

    @Autowired
    private SponsorService sponsorService;

    public Mapping() {
        modelMapper = new ModelMapper();
        /*modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);
        Converter<UserDTO, User> converter = context -> {
            Sponsor sponsor = sponsorService.findSponsorById(context.getSource().getSponsorId());
            context.getDestination().setSponsor(sponsor);
            //return context.getDestination();
        };
        modelMapper.createTypeMap(UserDTO.class, User.class)
                .setPreConverter(converter);*/
    }

    public ModelMapper getModelMapper() {
        return modelMapper;
    }
}
