package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.worker.processor.JobDataWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class ContractData {

    private final JobDataWriter writer;
    private final Contract contract;
    private final ProgressTracker progressTracker;
    private final OffsetDateTime attestedTime;

}
