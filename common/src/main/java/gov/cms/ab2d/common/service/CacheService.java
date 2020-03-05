package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.ClearCoverageCacheRequest;

public interface CacheService {

    void clearCache(ClearCoverageCacheRequest request);
}
