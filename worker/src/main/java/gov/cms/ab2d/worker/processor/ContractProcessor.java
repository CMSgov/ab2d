package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;

import java.nio.file.Path;
import java.util.List;

public interface ContractProcessor {
    List<JobOutput> process(Path outputDirPath, ContractData contractData);
}
