package gov.cms.ab2d.worker.slice;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;

import java.util.List;
import java.util.Map;

public interface PatientSliceCreator {

    Map<Integer, List<PatientDTO>> createSlices(List<PatientDTO> patients);
}
