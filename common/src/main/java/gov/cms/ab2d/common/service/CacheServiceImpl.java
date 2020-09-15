package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.ClearCoverageCacheRequest;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.CoverageRepository;
import gov.cms.ab2d.common.repository.CoveragePeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static gov.cms.ab2d.common.util.Constants.AB2D_EPOCH;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {
    private static final String DEFAULT_MESG = "rows deleted from coverage table for";

    private final ContractRepository contractRepo;
    private final CoverageRepository coverageRepo;
    private final CoveragePeriodRepository coverageSearchRepo;

    @Override
    @Transactional
    public void clearCache(ClearCoverageCacheRequest request) {

        final Integer month = getMonth(request);
        final Integer year = getYear(request);
        final String contractNumber = request.getContractNumber();

        final boolean hasMonth = month != null;
        final boolean hasYear = year != null;
        final boolean hasContractNumber = StringUtils.isNotBlank(contractNumber);
        Long contractId = getContractId(contractNumber, hasContractNumber);

        int deletedCount = 0;
        if (hasMonth && hasContractNumber && hasYear) {
            CoveragePeriod coveragePeriod = coverageSearchRepo.getByContractIdAndMonthAndYear(contractId, month, year);
            deletedCount = coverageRepo.removeAllByCoveragePeriod(coveragePeriod);
            log.info("[{}] {} contractNumber:[{}] and month-year:[{}-{}]", deletedCount, DEFAULT_MESG, contractNumber, month, year);

        } else if (hasContractNumber) {
            List<CoveragePeriod> coveragePeriodIds = coverageSearchRepo.findAllByContractId(contractId);

            for (CoveragePeriod coveragePeriod : coveragePeriodIds) {
                deletedCount = coverageRepo.removeAllByCoveragePeriod(coveragePeriod);
                log.info("[{}] {} contractNumber:[{}]", deletedCount, DEFAULT_MESG, contractNumber);
            }

        } else if (hasMonth && hasYear) {
            List<CoveragePeriod> coveragePeriods = coverageSearchRepo.findAllByMonthAndYear(month, year);

            for (CoveragePeriod coveragePeriod : coveragePeriods) {
                deletedCount = coverageRepo.removeAllByCoveragePeriod(coveragePeriod);
                log.info("[{}] {} month-year:[{}-{}]", deletedCount, DEFAULT_MESG, month, year);
            }
        }

        if (deletedCount == 0) {
            log.info("No records found in the coverage cache for the given criteria");
        }
    }

    private Integer getMonth(ClearCoverageCacheRequest request) {
        final Integer month = request.getMonth();
        if (month != null && (month < 1 || month > 12)) {
            final String errMsg = "invalid value for month. Month must be between 1 and 12";
            log.error("{} - invalid month :[{}]", errMsg, month);
            throw new InvalidUserInputException(errMsg);
        }
        return month;
    }

    private Integer getYear(ClearCoverageCacheRequest request) {
        final Integer year = request.getYear();

        final int currentYear = OffsetDateTime.now(ZoneOffset.UTC).getYear();

        if (year != null && (year < AB2D_EPOCH || year > currentYear)) {
            final String errMsg = "invalid value for year. Year must be between " + AB2D_EPOCH + " and " + currentYear;
            log.error("{} - invalid month :[{}]", errMsg, year);
            throw new InvalidUserInputException(errMsg);
        }
        return year;
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
