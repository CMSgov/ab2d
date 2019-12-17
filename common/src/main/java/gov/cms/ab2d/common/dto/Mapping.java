package gov.cms.ab2d.common.dto;

import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.service.SponsorService;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;

public class Mapping {

    private final ModelMapper modelMapper;

    @Autowired
    private SponsorService sponsorService;

    public Mapping() {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);
        PropertyMap<UserDTO, User> userMap = new PropertyMap<>() {
            protected void configure() {
                Sponsor sponsor = sponsorService.findSponsorById(source.getSponsorId());
                map().setSponsor(sponsor);
            }
        };
        modelMapper.addMappings(userMap);
    }

    public ModelMapper getModelMapper() {
        return modelMapper;
    }
}
