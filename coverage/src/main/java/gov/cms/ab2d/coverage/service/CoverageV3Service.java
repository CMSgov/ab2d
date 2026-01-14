package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.coverage.model.CoverageV3Periods;
import gov.cms.ab2d.coverage.util.CoverageV3Utils;

public interface CoverageV3Service {

    int countBeneficiariesByCoveragePeriod(CoverageV3Periods coveragePeriods, String contract);

}
