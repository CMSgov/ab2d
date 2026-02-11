package gov.cms.ab2d.coverage.repository.v3;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.Assert.assertEquals;

@ExtendWith(SpringExtension.class)
@EnableJpaRepositories(basePackages = {"gov.cms.ab2d.coverage.repository.v3"})
@ContextConfiguration(classes = {CoverageV3JpaConfig.class})
class CoverageV3HistoricalRepositoryTest {

    @Autowired
    CoverageV3HistoricalRepository coverageV3HistoricalRepository;

    @Test
    void findAllByMonthAndYear() {
        val periods = coverageV3HistoricalRepository.findAllByMonthAndYear(6, 2025);
        assertEquals(1, periods.size());
    }

    @Test
    void findByContractAndMonthAndYear() {
        val periods = coverageV3HistoricalRepository.findByContractAndMonthAndYear("Z0000", 6, 2025);
        assertEquals(1, periods.size());
    }

    @Test
    void findAllByContract() {
        val periods = coverageV3HistoricalRepository.findAllByContract("Z0000");
        assertEquals(9, periods.size());
    }

}
