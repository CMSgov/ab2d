package gov.cms.ab2d.worker.processor.coverage.check;

import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.worker.model.ContractWorkerDto;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;

@SuppressWarnings("checkstyle:VisibilityModifier")
@AllArgsConstructor
public abstract class CoverageCheckPredicate implements Predicate<ContractWorkerDto> {

    protected final CoverageService coverageService;
    protected final Map<String, List<CoverageCount>> coverageCounts;
    protected final List<String> issues;
}
