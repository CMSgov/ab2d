package gov.cms.ab2d.coverage.repository.v3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@EnableJpaRepositories(basePackages = {"gov.cms.ab2d.coverage.repository.v3"})
@ContextConfiguration(classes = {CoverageV3JpaConfig.class})
class CoverageV3HistoricalRepositoryTest {

    @Autowired
    CoverageV3HistoricalRepository coverageV3HistoricalRepository;

//    @Test
//    void findAllByMonthAndYear() {
//        coverageV3HistoricalRepository.findAllByMonthAndYear();
//    }
//
//    @Test
//    void findByContractAndMonthAndYear() {
//        coverageV3HistoricalRepository.findByContractAndMonthAndYear();
//    }
//
//    @Test
//    void findAllByContract() {
//        coverageV3HistoricalRepository.findAllByContract();
//    }

}
