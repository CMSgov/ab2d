package gov.cms.ab2d.coverage.query;

import lombok.val;
import org.junit.jupiter.api.Test;
import java.time.ZonedDateTime;

import static gov.cms.ab2d.coverage.query.TempTestUtils.devDataSource;
import static gov.cms.ab2d.coverage.util.CoverageV3Utils.*;

class CountBeneficiariesByCoveragePeriodsTest {

    //@Test
    void test() {
        var startDateTime = ZonedDateTime.now().minusYears(10);
        val endDateTime = ZonedDateTime.now().plusMonths(2);
        val periods = enumerateCoveragePeriods(startDateTime, endDateTime);
        int count = new CountBeneficiariesByCoveragePeriods(devDataSource()).countBeneficiaries("Z0000", periods, false);
        System.out.println(count);
    }
}
