package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.coverage.model.YearMonthRecord;

import java.util.List;

public interface CoverageV3Service {

    int countBeneficiariesByCoveragePeriod(List<YearMonthRecord> yearMonthRecords, String contract);

}
