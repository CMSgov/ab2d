package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.coverage.model.YearMonthRecord;
import gov.cms.ab2d.coverage.repository.CoverageV3HistoricalRepository;
import gov.cms.ab2d.coverage.repository.CoverageV3Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class CoverageV3ServiceImpl implements CoverageV3Service {

    private final CoverageV3Repository coverageV3Repository;
    private final CoverageV3HistoricalRepository coverageV3HistoricalRepository;

    public CoverageV3ServiceImpl(CoverageV3Repository coverageV3Repository, CoverageV3HistoricalRepository coverageV3HistoricalRepository) {
        this.coverageV3Repository = coverageV3Repository;
        this.coverageV3HistoricalRepository = coverageV3HistoricalRepository;
    }

    public int countBeneficiariesByCoveragePeriod(List<YearMonthRecord> yearMonthRecords, final String contract) {
        try {
            log.info("Count #1: {}", coverageV3Repository.getCountByContractAndYearMonthRecords(contract, yearMonthRecords));
            log.info("Count #2: {}", coverageV3HistoricalRepository.getCountByContractAndYearMonthRecords(contract, yearMonthRecords));
        }
        catch (Exception e) {
            log.error("Error", e);
            throw e;
        }
        return -1;
    }
    
}
