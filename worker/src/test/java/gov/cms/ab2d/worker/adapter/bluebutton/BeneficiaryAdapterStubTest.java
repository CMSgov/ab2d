package gov.cms.ab2d.worker.adapter.bluebutton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

class BeneficiaryAdapterStubTest {
    private BeneficiaryAdapterStub cut;

    @BeforeEach
    void setup() {
        cut = new BeneficiaryAdapterStub();
    }

    @Test
    void when_0000_returns_000() {

        var patients = cut.getPatientsByContract("S0000").getPatients();
        assertThat(patients.size(), is(000));
    }

    @Test
    void when_0001_returns_1_000() {
        var patients = cut.getPatientsByContract("S0001").getPatients();
        assertThat(patients.size(), is(1_000));
    }

    @Test
    void when_0002_returns_2_000() {
        var patients = cut.getPatientsByContract("S0002").getPatients();
        assertThat(patients.size(), is(2_000));
    }

    @Test
    void when_0010_returns_10_000() {
        var patients = cut.getPatientsByContract("S0010").getPatients();
        assertThat(patients.size(), is(10_000));
    }

    @Test
    void when_0030_returns_30_000() {
        var patients = cut.getPatientsByContract("S0030").getPatients();
        assertThat(patients.size(), is(30_000));
    }

    @Test
    void when_0031_returns_31_000() {
        var patients = cut.getPatientsByContract("S0031").getPatients();
        assertThat(patients.size(), is(31_000));
    }

    @Test
    void when_0032_returns_32_000() {
        var patients = cut.getPatientsByContract("S0032").getPatients();
        assertThat(patients.size(), is(32_000));
    }

    @Test
    void when_0060_returns_60_000() {
        var patients = cut.getPatientsByContract("S0060").getPatients();
        assertThat(patients.size(), is(60_000));
    }

    @Test
    void when_0090_returns_90_000() {
        var patients = cut.getPatientsByContract("S0090").getPatients();
        assertThat(patients.size(), is(90_000));
    }

    @Test
    void when_0100_returns_100_000() {
        var patients = cut.getPatientsByContract("S0100").getPatients();
        assertThat(patients.size(), is(100_000));
    }

    @Test
    void when_1_000_returns_1_000_000() {
        var patients = cut.getPatientsByContract("S1000").getPatients();
        assertThat(patients.size(), is(1_000_000));
    }


    @Test
    void when_5_000_returns_5_000_000() {
        var patients = cut.getPatientsByContract("S5000").getPatients();
        assertThat(patients.size(), is(5_000_000));
    }

    @Test
    @Disabled("Test takes too long to run")
    void when_9_999_returns_9_999_000() {
        var patients = cut.getPatientsByContract("S9999").getPatients();
        assertThat(patients.size(), is(9_999_000));
    }


    @Test
    void when_10_000_returns_000() {

        var patients = cut.getPatientsByContract("S10000").getPatients();
        assertThat(patients.size(), is(000));
    }
}