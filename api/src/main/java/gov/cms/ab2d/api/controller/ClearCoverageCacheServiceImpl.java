package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.CoverageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClearCoverageCacheServiceImpl implements ClearCoverageCacheService {
    private static final String DEFAULT_MESG = "rows deleted from coverage table for";

    private final ContractRepository contractRepo;
    private final CoverageRepository coverageRepo;

    @Override
    @Transactional
    public void clearCache(ClearCoverageCacheRequest request) {

        final Integer month = getMonth(request);
        final String contractNumber = request.getContractNumber();

        final boolean hasMonth = month != null;
        final boolean hasContractNumber = StringUtils.isNotBlank(contractNumber);
        Long contractId = getContractId(contractNumber, hasContractNumber);

        int deletedCount = 0;
        if (hasMonth && hasContractNumber) {
            deletedCount = coverageRepo.deleteInBulk(contractId, month);
            log.info("[{}] {} contractNumber:[{}] and month:[{}]", deletedCount, DEFAULT_MESG, contractNumber, month);

        } else if (hasContractNumber) {
            deletedCount = coverageRepo.deleteInBulk(contractId);
            log.info("[{}] {} contractNumber:[{}]", deletedCount, DEFAULT_MESG, contractNumber);

        } else if (hasMonth) {
            deletedCount = coverageRepo.deleteInBulk(month);
            log.info("[{}] {} month:[{}]", deletedCount, DEFAULT_MESG, month);
        }

        if (deletedCount > 0) {
            log.info("Coverage Cache cleared");
        } else {
            log.info("No records found in the coverage cache for the given criteria");
        }
    }

    private Integer getMonth(ClearCoverageCacheRequest request) {
        final Integer month = request.getMonth();
        if (month != null) {
            if (month < 1 || month > 12) {
                final String errMsg = "invalid value for month. Month must be between 1 and 12";
                log.error("{} - invalid month  :[{}]", errMsg, month);
                throw new InvalidUserInputException(errMsg);
            }
        }
        return month;
    }

    private Long getContractId(String contractNumber, boolean hasContractNumber) {
        if (!hasContractNumber) {
            return null;
        }

        final Optional<Contract> optContract = contractRepo.findContractByContractNumber(contractNumber);
        if (optContract.isPresent()) {
            return optContract.get().getId();
        }

        final String errMsg = "Contract not found";
        log.error("{} - contractNumber :[{}]", errMsg, contractNumber);
        throw new InvalidUserInputException(errMsg);
    }


}
