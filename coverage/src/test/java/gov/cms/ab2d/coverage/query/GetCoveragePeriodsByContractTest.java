package gov.cms.ab2d.coverage.query;

import lombok.val;
import org.junit.jupiter.api.Test;


import static gov.cms.ab2d.coverage.query.TempTestUtils.devDataSource;

class GetCoveragePeriodsByContractTest {

    //@Test
    void test() {
        val result = new GetCoveragePeriodsByContract(devDataSource()).getCoveragePeriodsForContract("Z0000");
        System.out.println(result);
    }

}
