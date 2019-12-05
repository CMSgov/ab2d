package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.repository.ConsentRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.io.InputStreamReader;

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

        cut.process();

        verify(mockS3Gateway).getS3Object();
    }


}