package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
public class OptOutTest {

    @Autowired
    private OptOutRepository optOutRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    public void cleanup() {
        optOutRepository.deleteAll();
    }

    @Test
    public void testOptOut() {
        OptOut oo = new OptOut();
        oo.setMbi("MBI");
        oo.setCcwId("CCW ID");
        LocalDate ld = LocalDate.now();
        oo.setEffectiveDate(ld);
        oo.setHicn("HICN");
        oo.setLoIncCode("LO INC CODE");
        oo.setPolicyCode("Policy COde");
        oo.setPurposeCode("Purpose COde");
        oo.setScopeCode("Scope COde");

        List<OptOut> noopts = optOutRepository.findByCcwId("CCW ID");
        assertEquals(0, noopts.size());

        optOutRepository.save(oo);
        List<OptOut> noopts2 = optOutRepository.findByCcwId("CCW ID");
        assertEquals(1, noopts2.size());

        assertEquals(oo.getMbi(), "MBI");
        assertEquals(oo.getCcwId(), "CCW ID");
        assertEquals(oo.getEffectiveDate(), ld);
        assertEquals(oo.getHicn(), "HICN");
        assertEquals(oo.getLoIncCode(), "LO INC CODE");
        assertEquals(oo.getPolicyCode(), "Policy COde");
        assertEquals(oo.getPurposeCode(), "Purpose COde");
        assertEquals(oo.getScopeCode(), "Scope COde");
    }
}
