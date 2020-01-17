package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.common.model.Contract;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

@Getter
@AllArgsConstructor
public class ContractData {

    private final Path outputDir;
    private final Contract contract;
    private final String jobUuid;
    private final List<WorkInProgress> workInProgressList;

}
