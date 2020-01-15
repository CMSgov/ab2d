package gov.cms.ab2d.worker.adapter.bluebutton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

class ContractAdapterStubTest {
    private ContractAdapterStub cut;

    @BeforeEach
    void setup() {
        cut = new ContractAdapterStub();
    }


    @Test
    @DisplayName("when contractNumber is 0, returns 10 patient records")
//    @DisplayName("when contractNumber is 0, returns 500 patient records")
    void when_0000_returns_000() {
        var patients = cut.getPatients("S0000").getPatients();
//        assertThat(patients.size(), is(500));
        assertThat(patients.size(), is(10));
    }

    @Test
    @DisplayName("when contractNumber is greater than 9999, returns empty list")
    void when_19999_returns_000() {
        var patients = cut.getPatients("S19999").getPatients();
        assertThat(patients.size(), is(0));
    }



    @DisplayName("Given ContractNumber, returns varying number of patient records")
    @ParameterizedTest(name = "Given ContractNumber \"{0}\" returns {1} patient records")
    @CsvSource({
//            "S0001, 1000",
            "S0001, 10",
            "S0002, 2000",
            "S0010, 10000",
            "S0030, 30000",
            "S0100, 100000",
            "S0110, 110000",
            "S1000, 1000000",
            "S2000, 2000000",
            "S5000, 5000000",
    })
    void when_contractNumber_returns_PatientCount(String contractNumber, int patientCount) {
        var patients = cut.getPatients(contractNumber).getPatients();
        assertThat(patients.size(), is(patientCount));
    }



    @Test
    @Disabled("Takes too long to run. But could be run locally as needed")
    @DisplayName("when contractNumber is greater than 9999, returns 9999000 patient records")
    void when_S9999_returns_9999000() {
        var patients = cut.getPatients("S9999").getPatients();
        assertThat(patients.size(), is(9999000));
    }
}