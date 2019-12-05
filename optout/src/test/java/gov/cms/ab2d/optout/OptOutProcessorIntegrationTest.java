package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.Consent;
import gov.cms.ab2d.common.repository.ConsentRepository;
import gov.cms.ab2d.optout.gateway.S3Gateway;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
class OptOutProcessorIntegrationTest {

    @MockBean
    private S3Gateway mockS3Gateway;

    @Autowired
    private ConsentRepository consentRepo;

    @Autowired
    private ConsentConverterService consentConverterSvc;

    private OptOutProcessor cut;


    @BeforeEach
    void setUp() {
        cut = new OptOutProcessorImpl(mockS3Gateway, consentRepo, consentConverterSvc);
    }

    @Test
    void process_shouldInsertRowsIntoConsentTable()  {

        final String testInputFile = "test-data/test-data.txt";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);
        final InputStreamReader isr = new InputStreamReader(inputStream);

        when(mockS3Gateway.getS3Object()).thenReturn(isr);

        final List<Consent> consentRowsBeforeProcessing = consentRepo.findAll();
        cut.process();
        final List<Consent> consentRowsAfterProcessing = consentRepo.findAll();

        assertThat(consentRowsBeforeProcessing, is(empty()));
        assertThat(consentRowsAfterProcessing, is(not(empty())));
        assertThat(consentRowsAfterProcessing.size(), is(9));
        verify(mockS3Gateway).getS3Object();
    }


}