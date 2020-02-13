package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@Slf4j
@SpringBootTest
@Testcontainers
class ContractAdapterTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();


    @Autowired
    private ContractAdapter cut;

//    @BeforeEach
//    void setup() {
//        cut = new ContractAdapterImpl();
//    }


    @Test
    @DisplayName("given contractNumber, get patients from BFD API")
    void getPatients() {
        if (cut == null) {
            log.error("cut is NULL");
        }
        var patients = cut.getPatients("S0000").getPatients();
        assertThat(patients.size(), is(100));
    }


}