package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.JobOutput;

import java.nio.file.Path;
import java.util.List;

public interface ContractProcessor {
    List<JobOutput> process(Path outputDirPath, JobData jobData);
}
