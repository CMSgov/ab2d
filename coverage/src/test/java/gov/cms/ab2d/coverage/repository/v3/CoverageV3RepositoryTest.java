package gov.cms.ab2d.coverage.repository.v3;

import gov.cms.ab2d.coverage.model.v3.CoverageV3;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.Assert.*;

@ExtendWith(SpringExtension.class)
@EnableJpaRepositories(basePackages = {"gov.cms.ab2d.coverage.repository.v3"})
@ContextConfiguration(classes = {CoverageV3JpaConfig.class})
class CoverageV3RepositoryTest {

    @Autowired
    CoverageV3Repository coverageV3Repository;

    @Test
    void findAllByMonthAndYear() {
        val periods = coverageV3Repository.findAllByMonthAndYear(1, 2026);
        assertEquals(5, periods.size());
    }

    @Test
    void findByContractAndMonthAndYear() {
        val periods = coverageV3Repository.findByContractAndMonthAndYear("Z0000", 1, 2026);
        assertEquals(3, periods.size());
    }

    @Test
    void findAllByContract() {
        val periods = coverageV3Repository.findAllByContract("Z0000");
        assertEquals(9, periods.size());
    }
}
