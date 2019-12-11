package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.optout.gateway.S3Gateway;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@Testcontainers
class OptOutProcessorIntegrationTest {

    @MockBean
    private S3Gateway mockS3Gateway;

    @Autowired
    private OptOutRepository optOutRepo;

    @Autowired
    private OptOutConverterService optOutConverterSvc;

    @Autowired
    private OptOutProcessor cut;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Test
    @Transactional
    void process_shouldInsertRowsIntoOptOutTable()  {

        final String testInputFile = "test-data/test-data.txt";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);
        final InputStreamReader isr = new InputStreamReader(inputStream);

        when(mockS3Gateway.getOptOutFile()).thenReturn(isr);

        final List<OptOut> optOutRowsBeforeProcessing = optOutRepo.findAll();
        cut.process();
        final List<OptOut> optOutRowsAfterProcessing = optOutRepo.findAll();

        assertThat(optOutRowsBeforeProcessing, is(empty()));
        assertThat(optOutRowsAfterProcessing, is(not(empty())));
        assertThat(optOutRowsAfterProcessing.size(), is(9));

        final OptOut optOut = optOutRepo.findByHicn("1000011403").get(0);
        assertThat(optOut.getPolicyCode(), is("OPTOUT"));
        assertThat(optOut.getPurposeCode(), is("TREAT"));
        assertThat(optOut.getScopeCode(), is("patient-privacy"));
        assertThat(optOut.getLoIncCode(), is("64292-6"));
        assertThat(optOut.getEffectiveDate(), is(LocalDate.of(2019,10,24)));

        verify(mockS3Gateway).getOptOutFile();
    }


}