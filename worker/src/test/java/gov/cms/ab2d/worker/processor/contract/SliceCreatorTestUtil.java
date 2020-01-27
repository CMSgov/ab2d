package gov.cms.ab2d.worker.processor.contract;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SliceCreatorTestUtil {


    public static List<PatientDTO> createPatients(int maxVal) {
        return IntStream.range(0, maxVal)
                .mapToObj(i -> toPatientDTO(i))
                .collect(Collectors.toList());
    }

    private static PatientDTO toPatientDTO(int i) {
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
}
