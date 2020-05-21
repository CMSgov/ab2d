package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.OptOutFile;

public interface OptOutFileService {

    OptOutFile findByFilename(String filename);
}
