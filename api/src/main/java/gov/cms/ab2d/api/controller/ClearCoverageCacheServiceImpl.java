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

    private final ContractRepository contractRepo;
    private final CoverageRepository coverageRepo;

    @Override
    @Transactional
    public void clearCache(ClearCoverageCacheRequest request) {
        var msg = "rows deleted from coverage table for";

        final Integer month = getMonth(request);
        final String contractNumber = request.getContractNumber();

        final boolean hasMonth = month != null;
        final boolean hasContractNumber = StringUtils.isNotBlank(contractNumber);
        Long contractId = getContractId(contractNumber, hasContractNumber);

        if (hasMonth && hasContractNumber) {

            final int deletedCount = coverageRepo.deleteInBulk(contractId, month);
            log.info("[{}] {} contractNumber:[{}] and month:[{}]", deletedCount, msg, contractNumber, month);

        } else if (hasContractNumber) {

            final int deletedCount = coverageRepo.deleteInBulk(contractId);
            log.info("[{}] {} contractNumber:[{}]", deletedCount, msg, contractNumber);

        } else if (hasMonth) {

            final int deletedCount = coverageRepo.deleteInBulk(month);
            log.info("[{}] {} month:[{}]", deletedCount, msg, month);

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
