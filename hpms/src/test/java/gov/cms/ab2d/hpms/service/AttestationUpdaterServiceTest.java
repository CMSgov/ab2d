package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.hpms.SpringBootTestApp;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = SpringBootTestApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
@Testcontainers
public class AttestationUpdaterServiceTest {

    @Autowired
    SponsorRepository sponsorRepository;

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Qualifier("for_testing")
    @Autowired
    private AttestationUpdaterServiceImpl aus;

    @Test
    public void contractUpdated() {
        assertNotNull(aus);
        aus.pollOrganizations();
        Optional<Sponsor> optSponser = sponsorRepository.findByHpmsIdAndOrgName(5, "ABC Org");
        assertTrue(optSponser.isPresent());
        Sponsor sponsor = optSponser.get();
        assertEquals(1, sponsor.getContracts().size());
    }

    @Test
    public void noNewContracts() {
        List<Contract> result = aus.addNewContracts(Lists.emptyList());
        assertTrue(result.isEmpty());
    }

    @TestConfiguration
    static class MockHpmsFetcherConfig {

        @Autowired
        private SponsorRepository sponsorRepository;

        @Autowired
        private ContractRepository contractRepository;

        @Autowired
        private PropertiesService propertiesService;

        @Qualifier("for_testing")
        @Bean()
        public AttestationUpdaterServiceImpl getMockService()
        {
            return new AttestationUpdaterServiceImpl(sponsorRepository, contractRepository,
                    new MockHpmsFetcher(), propertiesService);
        }
    }

}
