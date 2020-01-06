package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.SponsorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
@Service
@Slf4j
public class SponsorServiceImpl implements SponsorService {

    @Autowired
    private SponsorRepository sponsorRepository;

    public Optional<Sponsor> findByHpmsIdAndParent(Integer hpmsId, Sponsor parentId) {
        return sponsorRepository.findByHpmsIdAndParent(hpmsId, parentId);
    }

    public Sponsor saveSponsor(Sponsor sponsor) {
        return sponsorRepository.saveAndFlush(sponsor);
    }

    public Sponsor findSponsorById(Long id) {
        return sponsorRepository.findById(id).orElseThrow(() -> {
            log.error("No sponsor found with ID {}", id);
            return new ResourceNotFoundException("No sponsor found with ID " + id);
        });
    }

    public Sponsor findByHpmsIdAndOrgName(Integer hpmsId, String orgName) {
        return sponsorRepository.findByHpmsIdAndOrgName(hpmsId, orgName).orElseThrow(() -> {
            log.error("No sponsor found with hpms ID {} and org name {}", hpmsId, orgName);
            return new ResourceNotFoundException("No sponsor found with hpms ID " +  hpmsId + " and org name " + orgName);
        });
    }
}
