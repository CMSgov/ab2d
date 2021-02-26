package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class ContractData {
    private final Contract contract;
    private final ProgressTracker progressTracker;
    private final OffsetDateTime sinceTime;
    private final String clientId;
}
