package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.coverage.model.CoverageContractDTO;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;

@Slf4j
public final class CoverageUtils {

    public static ZonedDateTime getEndDateTime() {
        // Assume current time zone is EST since all deployments are in EST
        ZonedDateTime now = ZonedDateTime.now(AB2D_ZONE);
        now = now.plusMonths(1);
        now = ZonedDateTime.of(now.getYear(), now.getMonthValue(),
                1, 0, 0, 0, 0,  AB2D_ZONE);
        return now;
    }

    public static ZonedDateTime getAttestationTime(CoverageContractDTO contract) {
        ZonedDateTime attestationTime = contract.getESTAttestationTime();

        // Force first coverage period to be after
        // January 1st 2020 which is the first moment we report data for
        if (attestationTime.isBefore(AB2D_EPOCH)) {
            log.info("contract attested before ab2d epoch setting to epoch");
            attestationTime = AB2D_EPOCH;
        }
        return attestationTime;
    }

}
