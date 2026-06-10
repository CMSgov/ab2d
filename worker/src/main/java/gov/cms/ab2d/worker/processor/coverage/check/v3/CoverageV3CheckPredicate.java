package gov.cms.ab2d.worker.processor.coverage.check.v3;

import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Count;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@SuppressWarnings("checkstyle:VisibilityModifier")
@AllArgsConstructor
public abstract class CoverageV3CheckPredicate implements Predicate<ContractDTO> {

    protected final CoverageV3Service coverageService;
    protected final Map<String, List<YearMonthRecord>> coverageCounts;
    protected final List<String> issues;
}
