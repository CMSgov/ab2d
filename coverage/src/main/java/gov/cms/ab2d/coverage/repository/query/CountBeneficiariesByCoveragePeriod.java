package gov.cms.ab2d.coverage.repository.query;

import gov.cms.ab2d.coverage.model.YearMonthRecord;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CountBeneficiariesByCoveragePeriod {
    private final List<YearMonthRecord> yearMonthRecords;

    

}
