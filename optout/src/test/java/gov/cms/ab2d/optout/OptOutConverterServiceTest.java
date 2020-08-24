package gov.cms.ab2d.optout;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Testcontainers
class OptOutConverterServiceTest {

    @InjectMocks
    private OptOutConverterService cut;

    @Mock
    private BFDClient client;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private static final String MBI_IDENTIFIER = "id1-us-mbi";

    private static final String PRE_1964_IDENTIFIER = "a123456";

    private static final String POST_1964_IDENTIFIER = "a123456789";

    @BeforeEach
    void setUp() {
        Bundle fakeBundle = new Bundle();
        Patient patient = new Patient();
        Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
        component.setResource(patient);
        // Creates new list;
        List<Bundle.BundleEntryComponent> entry = fakeBundle.getEntry();
        entry.add(component);
        cut = new OptOutConverterServiceImpl();

        Bundle mbiBundle = new Bundle();
        Patient mbiPatient = new Patient();
        Identifier identifier = new Identifier();
        identifier.setSystem(MBI_IDENTIFIER);
        identifier.setValue(MBI_IDENTIFIER);
        mbiPatient.setIdentifier(List.of(identifier));
        Bundle.BundleEntryComponent mbiComponent = new Bundle.BundleEntryComponent();
        mbiComponent.setResource(mbiPatient);
        List<Bundle.BundleEntryComponent> mbiEntry = mbiBundle.getEntry();
        mbiEntry.add(mbiComponent);

        MockitoAnnotations.initMocks(this);
        when(client.requestPatientByHICN("1000011403")).thenReturn(fakeBundle);
        when(client.requestPatientByHICN(PRE_1964_IDENTIFIER)).thenReturn(fakeBundle);
        when(client.requestPatientByHICN(POST_1964_IDENTIFIER)).thenReturn(fakeBundle);
        when(client.requestPatientByMBI(MBI_IDENTIFIER)).thenReturn(mbiBundle);
    }

    @Test
    @DisplayName("when header, should skip line and not create a opt_out record")
    void whenHeader_shouldNotCreateOptOut() {
        final List<OptOut> optionalOptOut = cut.convert("HDR_BENEDATASHR20191029");
        assertTrue(optionalOptOut.isEmpty());
    }

    @Test
    @DisplayName("when trailer, should skip line and not create a opt_out record")
    void whenTrailer_shouldNotCreateOptOut() {
        final List<OptOut> optionalOptOut = cut.convert("TRL_BENEDATASHR2019102930");
        assertTrue(optionalOptOut.isEmpty());
    }

    @Test
    @DisplayName("when preference-indicator is not opt-out, should skip line and not create a opt_out record")
    void whenPreferenceIndicatorIsNotOptOut_shouldNotCreateOptOut() {
        final String line = getLinesFromFile().skip(7).limit(1).collect(Collectors.toList()).get(0);

        final List<OptOut> optionalOptOut = cut.convert(line);
        assertTrue(optionalOptOut.isEmpty());
    }

    @Test
    @DisplayName("when source-code is blank, should skip line and not create a opt_out record")
    void whenSourceCodeisBlank_shouldNotCreateOptOut() {
        final String line = getLinesFromFile().skip(1).limit(1).collect(Collectors.toList()).get(0);

        final List<OptOut> optionalOptOut = cut.convert(line);
        assertTrue(optionalOptOut.isEmpty());
    }

    @Test
    @DisplayName("when source-code is invalid, should throw an exception")
    void whenSourceCodeIsInvalid_shouldThrowException() {

        // Valid source code is 1800. Replacing it with 1888 for this test case.
        final String badSourceCode = "1888";

        final String goodLine = getLinesFromFile().skip(6).limit(1).collect(Collectors.toList()).get(0);
        final String lineWithInvalidSourceCode = goodLine.substring(0, 362) + badSourceCode + goodLine.substring(366);

        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.convert(lineWithInvalidSourceCode));

        assertThat(exceptionThrown.getMessage(), startsWith("Invalid data sharing source code"));
    }

    @Test
    @DisplayName("when effective date has invalid value, should throw an exception")
    void whenEffectiveDateValueIsInvalid_shouldThrowException() {

        // replace effective date with an invalid date for this test case
        final String badDate = "20192054";

        final String goodLine = getLinesFromFile().skip(6).limit(1).collect(Collectors.toList()).get(0);
        final String lineWithInvalidDate = goodLine.substring(0, 354) + badDate + goodLine.substring(362);

        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.convert(lineWithInvalidDate));

        assertThat(exceptionThrown.getMessage(), startsWith("Invalid Date"));
    }

    @Test
    @DisplayName("given line with valid data, should create a opt_out record")
    void whenValidData_shouldCreateOptOutRecordWithHICN() {
        final String line = getLinesFromFile().skip(6).limit(1).collect(Collectors.toList()).get(0);

        final List<OptOut> optionalOptOut = cut.convert(line);
        assertFalse(optionalOptOut.isEmpty());

        OptOut optOut = optionalOptOut.get(0);
        Assert.assertNotNull(optOut.getHicn());
    }

    @Test
    @DisplayName("given line with valid data, should create a opt_out record")
    void whenValidData_shouldCreateOptOutRecordWithHICNPre1964() {
        final String line = getLinesFromFile().skip(9).limit(1).collect(Collectors.toList()).get(0);

        final List<OptOut> optionalOptOut = cut.convert(line);
        assertFalse(optionalOptOut.isEmpty());

        OptOut optOut = optionalOptOut.get(0);
        Assert.assertNotNull(optOut.getHicn());
    }

    @Test
    @DisplayName("given line with valid data, should create a opt_out record")
    void whenValidData_shouldCreateOptOutRecordWithHICNPost1964() {
        final String line = getLinesFromFile().skip(10).limit(1).collect(Collectors.toList()).get(0);

        final List<OptOut> optionalOptOut = cut.convert(line);
        assertFalse(optionalOptOut.isEmpty());

        OptOut optOut = optionalOptOut.get(0);
        Assert.assertNotNull(optOut.getHicn());
    }

    @Test
    @DisplayName("given line with valid data, should create a opt_out record")
    void whenValidData_shouldCreateOptOutRecordWithMBI() {
        final String line = getLinesFromFile().skip(8).limit(1).collect(Collectors.toList()).get(0);

        final List<OptOut> optionalOptOut = cut.convert(line);
        assertFalse(optionalOptOut.isEmpty());

        OptOut optOut = optionalOptOut.get(0);
        Assert.assertNull(optOut.getHicn());
        Assert.assertEquals(optOut.getMbi(), MBI_IDENTIFIER);
    }

    private Stream<String> getLinesFromFile() {
        final String testInputFile = "test-data.txt";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);
        final InputStreamReader isr = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(isr);
        return bufferedReader.lines();
    }
}