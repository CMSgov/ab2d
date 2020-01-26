package gov.cms.ab2d.worker.processor.contract;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.processor.contract.ContractSliceCreator;
import gov.cms.ab2d.worker.processor.contract.ContractSliceCreatorImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@Slf4j
class ContractSliceCreatorTest {


    private ContractSliceCreator cut;

    @BeforeEach
    void setup() {
        cut = new ContractSliceCreatorImpl();
        ReflectionTestUtils.setField(cut, "patientsPerFileLimit", 100);
        ReflectionTestUtils.setField(cut, "bdfConcurrencyLimit", 5);
    }


    @DisplayName("Given list of patients, returns smaller slices of patients")
    @ParameterizedTest(name = "Given [{0}] patient, returns [{1}] slices containing [{2}] patients,;except last slice which contains [{3}] patients")
    @CsvSource({
            "  69,  5,  13,  17",
            " 300,  5,  60,  60",
            " 500,  5, 100, 100",
            " 550,  6,  92,  90",
            " 600,  6, 100, 100",
            " 601,  7,  86,  85",
            " 650,  7,  93,  92",
            "5055, 51, 100,  55",
    })
    void given_list_of_patients_returns_slices(int maxValue, int expectedRows, int expectedSliceSize, int expectedLastSliceSize) {
        var patients = getPatientsList(maxValue);
        var result = cut.createSlices(patients);
        checkResults(result, expectedRows, expectedSliceSize, expectedLastSliceSize);
    }


    @DisplayName("Given list of patients, returns smaller slices of patients")
    @ParameterizedTest(name = "Given [{0}] patient, returns [{1}] slices containing [{2}] patients; except last slice which contains [{3}] patients")
    @CsvSource({
            "501,  6,  84,  81", "502,  6,  84,  82", "503,  6,  84,  83", "504,  6,  84,  84", "505,  6,  85,  80",
            "506,  6,  85,  81", "507,  6,  85,  82", "508,  6,  85,  83", "509,  6,  85,  84", "510,  6,  85,  85",
            "511,  6,  86,  81", "512,  6,  86,  82", "513,  6,  86,  83", "514,  6,  86,  84", "515,  6,  86,  85",
            "516,  6,  86,  86", "517,  6,  87,  82", "518,  6,  87,  83", "519,  6,  87,  84", "520,  6,  87,  85",
            "521,  6,  87,  86", "522,  6,  87,  87", "523,  6,  88,  83", "524,  6,  88,  84", "525,  6,  88,  85",
            "526,  6,  88,  86", "527,  6,  88,  87", "528,  6,  88,  88", "529,  6,  89,  84", "530,  6,  89,  85",
            "531,  6,  89,  86", "532,  6,  89,  87", "533,  6,  89,  88", "534,  6,  89,  89", "535,  6,  90,  85",
            "536,  6,  90,  86", "537,  6,  90,  87", "538,  6,  90,  88", "539,  6,  90,  89", "540,  6,  90,  90",
            "541,  6,  91,  86", "542,  6,  91,  87", "543,  6,  91,  88", "544,  6,  91,  89", "545,  6,  91,  90",
            "546,  6,  91,  91", "547,  6,  92,  87", "548,  6,  92,  88", "549,  6,  92,  89", "550,  6,  92,  90",
    })
    void given_501_550_patients_returns_slices(int maxValue, int expectedRows, int expectedSliceSize, int expectedLastSliceSize) {
        var patients = getPatientsList(maxValue);
        var result = cut.createSlices(patients);
        checkResults(result, expectedRows, expectedSliceSize, expectedLastSliceSize);
    }




    private List<PatientDTO> getPatientsList(int maxVal) {
        return IntStream.range(0, maxVal)
                .mapToObj(i -> toPatientDTO(i))
                .collect(Collectors.toList());
    }

    private PatientDTO toPatientDTO(int i) {
        final PatientDTO patientDTO = PatientDTO.builder()
                .patientId("S00000" + i)
                .build();

        var monthsUnderContract = new HashSet<Integer>();
        Random random = new Random();
        final int maxMonths = random.nextInt(12);
        for (var month = 0; month < maxMonths; ++month) {
            monthsUnderContract.add(random.nextInt(12));
        }

        patientDTO.setMonthsUnderContract(new ArrayList<>(monthsUnderContract));
        return patientDTO;
    }

    private void checkResults(Map<Integer, List<PatientDTO>> result, int expectedRows, int expectedSliceSize, int expectedLastSliceSize) {
        int rowsInMap = result.size();
        assertThat(rowsInMap, is(expectedRows));

        final List<PatientDTO> entry = result.get(1);
        assertThat(entry.size(), is(expectedSliceSize));

        if (rowsInMap > 1) {
            final List<PatientDTO> lastEntry = result.get(rowsInMap - 1);
            assertThat(lastEntry.size(), is(expectedLastSliceSize));
        }
    }


    @Test
    void givenPatientCountLessThanConcurrencyLimit_returns_1_slice() {
        var patients = getPatientsList(3);
        var result = cut.createSlices(patients);
        logResult(result);
        checkResults(result, 1, 3, 3);
    }

    @Test
    void sample_test_001() {
        var patients = getPatientsList(502);
        var result = cut.createSlices(patients);
        logResult(result);
        checkResults(result, 6, 84, 82);
    }


    private void logResult(Map<Integer, List<PatientDTO>> result) {
        final Set<Map.Entry<Integer, List<PatientDTO>>> entries = result.entrySet();
        for (Map.Entry<Integer, List<PatientDTO>> entry : entries) {
            log.info("key:{} - value:{}", entry.getKey(), entry.getValue());
        }
    }



    @Test
    void when_patient_count_is_less_than_100_returns_5_slices() {
        var patients = getPatientsList(60);
        var result = cut.createSlices(patients);
        checkResults(result, 5, 12, 12);
    }



    @Test
    void when_patient_count_is_69_returns_5_slices() {
        var patients = getPatientsList(69);
        var result = cut.createSlices(patients);
        checkResults(result, 5, 13, 17);
    }

    @Test
    void when_patient_count_is_300_returns_5_slices() {
        var patients = getPatientsList(300);
        var result = cut.createSlices(patients);
        checkResults(result, 5, 60, 60);
    }

    @Test
    void when_patient_count_is_500_returns_5_slices() {
        var patients = getPatientsList(500);
        var result = cut.createSlices(patients);
        checkResults(result, 5, 100, 100);
    }

    @Test
    void when_patient_count_is_501_returns_5_slices() {
        var patients = getPatientsList(501);
        var result = cut.createSlices(patients);
        checkResults(result, 6, 84, 81);
    }

    @Test
    void when_patient_count_is_greater_550__returns_6_slices() {
        var patients = getPatientsList(550);
        var result = cut.createSlices(patients);
        checkResults(result, 6, 92, 90);
    }

    @Test
    void when_patient_count_is_greater_600__returns_6_slices() {
        var patients = getPatientsList(600);
        var result = cut.createSlices(patients);
        checkResults(result, 6, 100, 100);
    }

    @Test
    void when_patient_count_is_greater_601__returns_6_slices() {
        var patients = getPatientsList(601);
        var result = cut.createSlices(patients);
        checkResults(result, 7, 86, 85);
    }

    @Test
    void when_patient_count_is_greater_650__returns_7_slices() {
        var patients = getPatientsList(650);
        var result = cut.createSlices(patients);
        checkResults(result, 7, 93, 92);
    }

    @Test
    void when_patient_count_is_greater_6050__returns_51_slices() {
        var patients = getPatientsList(5055);
        var result = cut.createSlices(patients);
        checkResults(result, 51, 100, 55);
    }


    @Test
    void when_patient_count_is_greater_6500__returns_52_slices() {
        var patients = getPatientsList(6152);
        var result = cut.createSlices(patients);
        checkResults(result, 62, 100, 52);
    }


}