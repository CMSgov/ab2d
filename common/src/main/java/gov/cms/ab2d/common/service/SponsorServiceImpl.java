package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.SponsorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
@Service
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
            return new ResourceNotFoundException("No sponsor found with ID " + id);
        });
    }
}
