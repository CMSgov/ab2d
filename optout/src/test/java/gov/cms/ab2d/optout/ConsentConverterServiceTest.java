package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.Consent;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class ConsentConverterServiceTest {

    private ConsentConverterService cut;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    void setUp() {
        cut = new ConsentConverterServiceImpl();
    }


    @Test
    @DisplayName("when header, should skip line and not create a consent record")
    void whenHeader_shouldNotCreateConsent() {
        final Optional<Consent> optConsent = cut.convert("HDR_BENEDATASHR20191029");
        assertTrue(optConsent.isEmpty());
    }


    @Test
    @DisplayName("when trailer, should skip line and not create a consent record")
    void whenTrailer_shouldNotCreateConsent() {
        final Optional<Consent> optConsent = cut.convert("TRL_BENEDATASHR2019102930");
        assertTrue(optConsent.isEmpty());
    }

    @Test
    @DisplayName("when preference-indicator is not opt-out, should skip line and not create a consent record")
    void whenPreferenceIndicatorIsNotOptOut_shouldNotCreateConsent() {
        final String line = getLinesFromFile().skip(7).limit(1).collect(Collectors.toList()).get(0);

        final Optional<Consent> optConsent = cut.convert(line);
        assertTrue(optConsent.isEmpty());
    }

    @Test
    @DisplayName("when source-code is blank, should skip line and not create a consent record")
    void whenSourceCodeisBlank_shouldNotCreateConsent() {
        final String line = getLinesFromFile().skip(1).limit(1).collect(Collectors.toList()).get(0);

        final Optional<Consent> optConsent = cut.convert(line);
        assertTrue(optConsent.isEmpty());
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
    @DisplayName("when HICN is invalid, should throw an exception")
    void whenHicnIsInvalid_shouldThrowException() {
        final String line = getLinesFromFile().skip(6).limit(1).collect(Collectors.toList()).get(0);

        //The first 9 characters of HICN must be numeric. Replacing 1st character with an alphabet for this test case.
        final String lineWithInvalidHicn = "A" + line.substring(1);

        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.convert(lineWithInvalidHicn));

        assertThat(exceptionThrown.getMessage(), startsWith("HICN does not match expected format"));
    }

    @Test
    @DisplayName("given line with valid data, should create a consent record")
    void whenValidData_shouldCreateConsentRecord() {
        final String line = getLinesFromFile().skip(6).limit(1).collect(Collectors.toList()).get(0);

        final Optional<Consent> optConsent = cut.convert(line);
        assertTrue(optConsent.isPresent());
    }


    private Stream<String> getLinesFromFile() {
        final String testInputFile = "test-data/test-data.txt";
        final InputStream inputStream = getClass().getResourceAsStream("/" + testInputFile);
        final InputStreamReader isr = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(isr);
        return bufferedReader.lines();
    }


}