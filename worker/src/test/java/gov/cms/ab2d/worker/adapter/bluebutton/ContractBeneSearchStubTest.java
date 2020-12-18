package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.worker.processor.eob.ProgressTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Month;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractBeneSearchStubTest {
    private ContractAdapterStub cut;
    private ProgressTracker tracker;

    private int currentMonth = Month.MARCH.getValue();

    @BeforeEach
    void setup() {
        cut = new ContractAdapterStub();
        tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .numContracts(1)
                .failureThreshold(1)
                .build();
    }

    @Test
    @DisplayName("when contractNumber is 0, returns 100 patient records")
    void when_0000_returns_100() {
        var patients = cut.getPatients("S0000", currentMonth, tracker).getPatients();
        assertThat(patients.size(), is(100));
    }

    @Test
    @DisplayName("when contractNumber is greater than 9999, returns empty list")
    void whenGreaterThan_9999_returns_000() {
        var patients = cut.getPatients("S19999", currentMonth, tracker).getPatients();
        assertThat(patients.size(), is(0));
    }

    @Test
    @DisplayName("Do error checking for extractContractSno")
    void testExtractContractSnoError() {
        assertEquals(0, cut.extractContractSno("AAAA"));
    }

    @DisplayName("Given ContractNumber, returns varying number of patient records")
    @ParameterizedTest(name = "Given ContractNumber \"{0}\" returns {1} patient records")
    @CsvSource({
            "S0001, 1000",
            "S0002, 2000",
            "S0010, 10000",
            "S0030, 30000",
            "S0100, 100000",
            "S0110, 110000"
    })
    void when_contractNumber_returns_PatientCount(String contractNumber, int patientCount) {
        Map<String, ContractBeneficiaries.PatientDTO> patients = cut.getPatients(contractNumber, currentMonth, tracker).getPatients();
        assertThat(patients.size(), is(patientCount));
    }
}