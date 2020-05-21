package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.OptOutFile;
import gov.cms.ab2d.common.repository.OptOutFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OptOutFileServiceImpl implements OptOutFileService {

    @Autowired
    private OptOutFileRepository optOutFileRepository;

    @Override
    public OptOutFile findByFilename(String filename) {
        return optOutFileRepository.findByFilename(filename).orElseThrow(() -> {
            throw new ResourceNotFoundException("Opt out filename " + filename + " was not present");
        });
    }
}
