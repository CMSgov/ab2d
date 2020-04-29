package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.eventlogger.EventLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OptOutImporterUnitTest {

    @Mock private OptOutConverterService converterService;
    @Mock private OptOutRepository optOutRepo;
    @Mock private EventLogger eventLogger;

    private OptOutImporter cut;

    @BeforeEach
    public void setup() {
        cut = new OptOutImporterImpl(optOutRepo, converterService, eventLogger);
    }

    @Test
    void process()  {
        var filename = "test-data.txt";
        var inputStream = getClass().getResourceAsStream("/" + filename);
        var isr = new InputStreamReader(inputStream);

        when(converterService.convert(any()))
                .thenReturn(List.of(createOptOut(filename)));

        cut.process(new BufferedReader(isr), filename);

        verify(optOutRepo, times(35)).findByCcwIdAndHicn(any(), any());
        verify(optOutRepo, times(35)).save(any());
        verify(converterService, times(35)).convert(any());
    }

    private OptOut createOptOut(String filename) {
        OptOut optout = new OptOut();
        optout.setHicn("hicn_" + Instant.now().getNano());
        optout.setCcwId("ccw_id_" + + Instant.now().getNano());
        optout.setEffectiveDate(LocalDate.now());
        optout.setFilename(filename);
        return optout;
    }
}