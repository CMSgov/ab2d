package gov.cms.ab2d.coverage.repository.v3;

import gov.cms.ab2d.coverage.model.v3.CoverageV3;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.Assert.assertEquals;

@ExtendWith(SpringExtension.class)
@EnableJpaRepositories(basePackages = {"gov.cms.ab2d.coverage.repository.v3"})
@ContextConfiguration(classes = {CoverageV3JpaConfig.class})
class CoverageV3RepositoryTest {

    @Autowired
    CoverageV3Repository coverageV3Repository;


    /*
    @Test
    void findAllByMonthAndYear() {
        coverageV3Repository.findAllByMonthAndYear();
    }

    @Test
    void findByContractAndMonthAndYear() {
        coverageV3Repository.findByContractAndMonthAndYear();
    }
     */

    @Test
    void findAllByContract() {
        val periods = coverageV3Repository.findAllByContract("Z0000");
        assertEquals(9, periods.size());


    }

}
