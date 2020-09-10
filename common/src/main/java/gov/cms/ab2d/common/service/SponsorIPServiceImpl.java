package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.config.Mapping;
import gov.cms.ab2d.common.dto.SponsorDTO;
import gov.cms.ab2d.common.dto.SponsorIPDTO;
import gov.cms.ab2d.common.model.PGInet;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.SponsorIP;
import gov.cms.ab2d.common.model.SponsorIPID;
import gov.cms.ab2d.common.repository.SponsorIPRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.modelmapper.MappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.UnknownHostException;
import java.util.Set;

@Service
@Transactional
@Slf4j
public class SponsorIPServiceImpl implements SponsorIPService {

    @Autowired
    private SponsorIPRepository sponsorIPRepository;

    @Autowired
    private Mapping mapping;

    @Override
    public SponsorIPDTO addIPAddress(SponsorIPDTO sponsorIPDTO) {
        checkIPs(sponsorIPDTO);

        Sponsor sponsor = getSponsor(sponsorIPDTO.getSponsor());

        SponsorIPDTO sponsorIPDTOReturn = new SponsorIPDTO();
        sponsorIPDTOReturn.setSponsor(sponsorIPDTO.getSponsor());

        SponsorIP sponsorIPRetrieved = null;
        for(String ip : sponsorIPDTO.getIps()) {
            sponsorIPRetrieved = sponsorIPRepository.save(new SponsorIP(new SponsorIPID(sponsor, createPGInet(ip))));
            sponsorIPDTOReturn.getIps().add(ip);
        }

        Sponsor sponsorRetrieved = sponsorIPRetrieved.getSponsorIPID().getSponsor();

        for(SponsorIP sponsorIP : sponsorRetrieved.getSponsorIPs()) {
            sponsorIPDTOReturn.getIps().add(sponsorIP.getSponsorIPID().getIpAddress().getAddress().getHostAddress());
        }

        return sponsorIPDTOReturn;
    }

    @Override
    public SponsorIPDTO removeIPAddresses(SponsorIPDTO sponsorIPDTO) {
        checkIPs(sponsorIPDTO);

        Sponsor sponsor = getSponsor(sponsorIPDTO.getSponsor());

        for(String ip : sponsorIPDTO.getIps()) {
            SponsorIPID sponsorIPID = new SponsorIPID(sponsor, createPGInet(ip));
            try {
                sponsorIPRepository.deleteById(sponsorIPID);
            } catch (DataAccessException dataAccessException) {
                log.error("Error deleting Sponsor IP with sponsor {} and IP {}", sponsor, ip);
                throw new ResourceNotFoundException("Error deleting Sponsor IP with sponsor " + sponsor + " and IP " + ip);
            }
            SponsorIP sponsorIP = new SponsorIP(sponsorIPID);
            sponsor.getSponsorIPs().remove(sponsorIP);
        }

        SponsorIPDTO sponsorIPDTOReturn = new SponsorIPDTO();
        sponsorIPDTOReturn.setSponsor(sponsorIPDTO.getSponsor());
        for(SponsorIP sponsorIP : sponsor.getSponsorIPs()) {
            sponsorIPDTOReturn.getIps().add(sponsorIP.getSponsorIPID().getIpAddress().getAddress().getHostAddress());
        }

        return sponsorIPDTOReturn;
    }

    @Override
    public SponsorIPDTO getIPs(SponsorDTO sponsorDTO) {
        Sponsor sponsor = getSponsor(sponsorDTO);

        Set<SponsorIP> sponsorIPs = sponsor.getSponsorIPs();

        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO();
        sponsorIPDTO.setSponsor(sponsorDTO);
        for(SponsorIP sponsorIP : sponsorIPs) {
            sponsorIPDTO.getIps().add(sponsorIP.getSponsorIPID().getIpAddress().getAddress().getHostAddress());
        }

        return sponsorIPDTO;
    }

    @SneakyThrows
    private Sponsor getSponsor(SponsorDTO sponsorDTO) {
        // A little silly, but if there's a ResourceNotFoundException ModelMapper wraps it with their own exception and throws it.
        try {
            return mapping.getModelMapper().map(sponsorDTO, Sponsor.class);
        } catch (MappingException mappingException) {
            throw ExceptionUtils.getRootCause(mappingException);
        }
    }

    private PGInet createPGInet(String ip) {
        try {
            return new PGInet(ip);
        } catch (UnknownHostException unknownHostException) {
            log.error("IP provided {} ip is not valid", ip);
            throw new InvalidUserInputException("IP provided " + ip + " is not valid");
        }
    }

    private void checkIPs(SponsorIPDTO sponsorIPDTO) {
        if(sponsorIPDTO.getIps().isEmpty()) {
            throw new InvalidUserInputException("IPs cannot be empty");
        }
    }
}
