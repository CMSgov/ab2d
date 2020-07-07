package gov.cms.ab2d.worker.service;

import java.util.Set;

public interface BeneficiaryService {

    Set<String> findPatientIdsInDb(Long contractId, int month);

    void storeBeneficiaries(Long contractId, Set<String> bfdPatientsIds, int month);
}
