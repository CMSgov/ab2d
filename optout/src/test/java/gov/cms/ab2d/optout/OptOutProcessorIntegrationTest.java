package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.Consent;
import gov.cms.ab2d.common.repository.ConsentRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
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
    private ConsentRepository consentRepo;

    @Autowired
    private ConsentConverterService consentConverterSvc;

    @Autowired
    private OptOutProcessor cut;

    @Container
    public static PostgreSQLContainer postgreSQLContainer = AB2DPostgresqlContainer.getInstance();

    @Test
    @Transactional
    void process_shouldInsertRowsIntoConsentTable()  {

        final String testInputFile = "test-data/test-data.txt";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);
        final InputStreamReader isr = new InputStreamReader(inputStream);

        when(mockS3Gateway.getOptOutFile()).thenReturn(isr);

        final List<Consent> consentRowsBeforeProcessing = consentRepo.findAll();
        cut.process();
        final List<Consent> consentRowsAfterProcessing = consentRepo.findAll();

        assertThat(consentRowsBeforeProcessing, is(empty()));
        assertThat(consentRowsAfterProcessing, is(not(empty())));
        assertThat(consentRowsAfterProcessing.size(), is(9));

        final Consent consent = consentRepo.findByHicn("1000011403").get(0);
        assertThat(consent.getPolicyCode(), is("OPTOUT"));
        assertThat(consent.getPurposeCode(), is("TREAT"));
        assertThat(consent.getScopeCode(), is("patient-privacy"));
        assertThat(consent.getLoIncCode(), is("64292-6"));
        assertThat(consent.getEffectiveDate(), is(LocalDate.of(2019,10,24)));

        verify(mockS3Gateway).getOptOutFile();
    }


}