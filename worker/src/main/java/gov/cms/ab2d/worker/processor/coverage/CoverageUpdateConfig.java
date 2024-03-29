package gov.cms.ab2d.worker.processor.coverage;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class CoverageUpdateConfig {

    // Number of months into the past to go looking to update
    private final int pastMonthsToUpdate;

    // Number of days that job is running
    private final int stuckHours;

    private final boolean override;
}
