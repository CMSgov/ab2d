package gov.cms.ab2d.coverage.query;

import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import lombok.val;
import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.coverage.query.TempTestUtils.devDataSource;

class GetCoverageMembershipTest {

    //@Test
    void test() {
        val years = CoverageServiceRepository.YEARS;
        val pageLimit=1000;
        val optOutOn=false;
        val contract="Z0000";
        boolean done = false;

        Long cursor = null;
        while (!done) {
            val result = new GetCoverageMembership(devDataSource())
                    .getCoverageMembership(contract, years, optOutOn, 1000, cursor);

            if (result.size() < pageLimit) {
                done=true;
            }
            else {
                cursor = result.get(result.size()-1).getIdentifiers().getPatientIdV3();
            }

            System.out.println(result);
        }
    }
}
