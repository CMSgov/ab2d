package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.model.OptOutFile;
import gov.cms.ab2d.common.repository.OptOutFileRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
public class OptOutFileServiceTest {

    @Autowired
    private OptOutFileRepository optOutFileRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Test
    public void testOptOutFile() {
        OptOutFile optOutFile = new OptOutFile();
        optOutFile.setFilename("filename");
        optOutFileRepository.save(optOutFile);

        Optional<OptOutFile> optOut = optOutFileRepository.findByFilename("filename");
        Assert.assertNotNull(optOut.get());
        Assert.assertEquals(optOut.get().getFilename(), "filename");
    }
}
