package gov.cms.ab2d.worker.config;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class CoverageMappingConfig {

    // Number of months into the past to go looking to update
    private final int pastMonthsToUpdate;

    // Number of days without a search before another search is required
    private final int staleDays;

    // Maximum attempts to complete mapping before failure
    private final int maxAttempts;

    // Number of days that job is running
    private final int stuckHours;
}
