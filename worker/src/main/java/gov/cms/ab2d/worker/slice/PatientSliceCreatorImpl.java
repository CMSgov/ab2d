package gov.cms.ab2d.worker.slice;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PatientSliceCreatorImpl implements PatientSliceCreator {

    @Value("${patients.per.file.limit:10000}")
    private int patientsPerFileLimit;

    @Value("${bfd.concurrency.limit:5}")
    private int bdfConcurrencyLimit;


    @Override
    public Map<Integer, List<PatientDTO>> createSlices(List<PatientDTO> patients) {
        final int patientCount = patients.size();
        final int optimalPatientCount = patientsPerFileLimit * bdfConcurrencyLimit;

        var slices = new HashMap<Integer, List<PatientDTO>>();
        if (patientCount < bdfConcurrencyLimit) {
            slices.put(1, patients);

        } else if (patientCount <= optimalPatientCount) {
            slices.putAll(sliceBelowOptimalPatientCount(patients));

        } else {
            slices.putAll(sliceAboveOptimalPatientCount(patients));
        }

        return slices;
    }


    private HashMap<Integer, List<PatientDTO>> sliceBelowOptimalPatientCount(List<PatientDTO> patients) {
        final var slices = new HashMap<Integer, List<PatientDTO>>();

        final int patientCount = patients.size();
        final int lastSlice = bdfConcurrencyLimit - 1;

        int sliceSize = patientCount / bdfConcurrencyLimit;
        for (int sliceSno = 0; sliceSno < bdfConcurrencyLimit; ++sliceSno) {
            int startOffset = sliceSno * sliceSize;

            if (sliceSno == lastSlice) {
                sliceSize = patientCount - startOffset;
            }

            var slice = doSlice(patients, sliceSize, startOffset);
            slices.put(sliceSno, slice);
        }

        return slices;
    }

    private HashMap<Integer, List<PatientDTO>> sliceAboveOptimalPatientCount(List<PatientDTO> patients) {
        final var slices = new HashMap<Integer, List<PatientDTO>>();

        final int patientCount = patients.size();
        final int sliceCount = (int) Math.ceil((patientCount * 1.0) / patientsPerFileLimit);
        final int sliceSize  = (int) Math.ceil((patientCount * 1.0) / sliceCount);

        for (int sliceSno = 0; sliceSno < sliceCount; ++sliceSno) {
            int startOffset = sliceSno * sliceSize;

            var slice = doSlice(patients, sliceSize, startOffset);
            slices.put(sliceSno, slice);
        }

        return slices;
    }

    private List<PatientDTO> doSlice(List<PatientDTO> patients, int sliceSize, int startOffset) {
        return patients.stream()
                .skip(startOffset)
                .limit(sliceSize)
                .collect(Collectors.toList());
    }


}
