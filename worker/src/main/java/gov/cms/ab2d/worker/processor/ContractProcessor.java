package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ContractProcessor {
    List<JobOutput> process(Path outputDirPath, Job job, Map<Long, CoverageSummary> patients);
}
