package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Beneficiary;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Coverage;
import gov.cms.ab2d.common.repository.BeneficiaryRepository;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.CoverageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneRepo;
    private final ContractRepository contractRepo;
    private final CoverageRepository coverageRepo;


    /**
     * Given a contractId and a month,
     * search for bene information in the local db first.
     *
     * @param contractId
     * @param month
     * @return
     */
    @Override
    public Set<String> findPatientIdsInDb(Long contractId, int month) {
        final List<String> patientIds = coverageRepo.findActivePatientIds(contractId, month);
        return new HashSet<>(patientIds);
    }



    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void storeBeneficiaries(Long contractId, Set<String> patientIds, int month) {
        final Contract contract = contractRepo.findById(contractId).get();
        patientIds.forEach(patientId -> storeBeneficiaryCoverage(contract, patientId, month));
    }


    private void storeBeneficiaryCoverage(Contract contract, String patientId, int month) {
        final Beneficiary beneficiary = getBeneficiary(patientId);
        final Coverage coverage = createCoverage(contract, beneficiary, month);
        contract.getCoverages().add(coverage);
        beneficiary.getCoverages().add(coverage);
    }


    private Beneficiary getBeneficiary(String patientId) {
        final Optional<Beneficiary> optPatient = beneRepo.findByPatientId(patientId);
        if (optPatient.isPresent()) {
            return optPatient.get();
        } else {
            return createBeneficiary(patientId);
        }

    }
    private Beneficiary createBeneficiary(String patientId) {
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setPatientId(patientId);
        return beneRepo.save(beneficiary);
    }

    private Coverage createCoverage(Contract contract, Beneficiary beneficiary, int month) {
        Coverage coverage = new Coverage();
        coverage.setContract(contract);
        coverage.setBeneficiary(beneficiary);
        coverage.setPartDMonth(month);
        return coverageRepo.save(coverage);
    }


}
